"""Build a prompt from a selected example and knowledge cards, then call the LLM."""

import json
import os
import re
from pathlib import Path
from urllib import error, request
from urllib.parse import urlsplit

from .knowledge import Card
from .scenarios import Scenario
from .curation import source_key


SYSTEM_PROMPT = Path(__file__).resolve().parents[1] / "system_prompt.txt"


class GenerationError(Exception):
    pass


def _address_user(payload: object) -> object:
    if not isinstance(payload, dict):
        return payload
    result = dict(payload)
    def convert(value: str) -> str:
        return re.sub(r"(?<!\w)(mình|tôi)(?!\w)",
                      lambda match: "Bạn" if match.group(0)[0].isupper() else "bạn",
                      value, flags=re.IGNORECASE)
    if isinstance(result.get("observation"), str):
        result["observation"] = convert(result["observation"])
    if isinstance(result.get("questions"), list):
        result["questions"] = [convert(item) if isinstance(item, str) else item
                               for item in result["questions"]]
    return result


def _response_content(raw: bytes, content_type: str) -> str:
    if "text/event-stream" not in content_type.casefold():
        return json.loads(raw)["choices"][0]["message"]["content"]
    pieces: list[str] = []
    for line in raw.decode("utf-8").splitlines():
        if not line.startswith("data:"):
            continue
        data = line[5:].strip()
        if data == "[DONE]":
            break
        if not data:
            continue
        event = json.loads(data)
        choices = event.get("choices", [])
        if not choices:
            continue
        first = choices[0]
        delta = first.get("delta") or first.get("message") or {}
        content = delta.get("content")
        if isinstance(content, str):
            pieces.append(content)
    if not pieces:
        raise GenerationError("API không trả nội dung câu hỏi")
    return "".join(pieces)


def validate_output(payload: object) -> dict[str, object]:
    if not isinstance(payload, dict) or set(payload) != {"observation", "questions"}:
        raise GenerationError("JSON phải có observation và questions")
    observation = payload["observation"]
    questions = payload["questions"]
    if not isinstance(observation, str) or not 100 <= len(observation.strip()) <= 1200:
        raise GenerationError("Đoạn gợi mở không hợp lệ")
    if not isinstance(questions, list) or len(questions) != 3:
        raise GenerationError("Cần đúng ba câu hỏi")
    clean_questions = []
    for question in questions:
        if not isinstance(question, str) or not 15 <= len(question.strip()) <= 240:
            raise GenerationError("Câu hỏi không hợp lệ")
        value = question.strip()
        if not value.endswith("?") or value.count("?") != 1:
            raise GenerationError("Mỗi mục phải là một câu hỏi")
        clean_questions.append(value)
    if len({re.sub(r"\W+", "", q.casefold()) for q in clean_questions}) != 3:
        raise GenerationError("Các câu hỏi bị trùng")
    combined = " ".join([observation, *clean_questions]).casefold()
    if re.search(r"(?<!\w)(mình|tôi)(?!\w)", combined):
        raise GenerationError("Đầu ra phải nói với người dùng bằng ngôi bạn")
    if not re.search(r"(?<!\w)bạn(?!\w)", observation.casefold()):
        raise GenerationError("Đoạn nhận định phải xưng bạn")
    if any(phrase in combined for phrase in ("bạn bị trầm cảm", "bạn mắc", "chắc chắn là", "nguyên nhân là")):
        raise GenerationError("Đầu ra có khẳng định không được hỗ trợ")
    return {"observation": observation.strip(), "questions": clean_questions}


def build_messages(scenario: Scenario, cards: tuple[Card, ...]) -> list[dict[str, str]]:
    selected = set(scenario.card_ids)
    knowledge = [{
        "id": card.id,
        "framework": card.framework,
        "concept": card.concept_vi,
        "definition": card.definition,
        "reflection_goal": card.reflection_goal,
        "application_contexts": card.contexts,
        "limitations": card.limitations,
        "question_templates": card.questions,
        "question_templates_origin": "generated_example_not_source_evidence",
        "review_status": card.review_status,
    } for card in cards if card.id in selected]
    if len(knowledge) != len(selected):
        raise ValueError(f"Thiếu knowledge card cho kịch bản {scenario.number}")
    content = {
        "scenario": {"number": scenario.number, "title": scenario.title, "input": scenario.input_text},
        "style_example_from_input_md": {
            "observation": scenario.example_observation,
            "questions": scenario.example_questions,
        },
        "knowledge_cards_from_json": knowledge,
    }
    return [
        {"role": "system", "content": SYSTEM_PROMPT.read_text(encoding="utf-8")},
        {"role": "user", "content": json.dumps(content, ensure_ascii=False)},
    ]


def provider_use_allowed(scenario: Scenario, cards: tuple[Card, ...]) -> bool:
    selected = [card for card in cards if card.id in scenario.card_ids]
    docs = Path(__file__).resolve().parents[1] / "docs"
    registry = json.loads((docs / "source_registry.json").read_text(encoding="utf-8"))["sources"]
    decisions = json.loads((docs / "card_decisions.json").read_text(encoding="utf-8"))["cards"]
    return len(selected) == len(scenario.card_ids) and not any(
            card.review_status != "approved"
            or decisions.get(card.id, {}).get("status") != "approved"
            or decisions.get(card.id, {}).get("citation_verified") is not True
            or registry[source_key(card.id)]["citation_review"] != "verified"
            or registry[source_key(card.id)]["rights"]["send_to_generation_provider"] != "approved"
            or not registry[source_key(card.id)].get("rights_evidence") for card in selected)


def generate(scenario: Scenario, cards: tuple[Card, ...]) -> dict[str, object]:
    if not provider_use_allowed(scenario, cards):
        raise GenerationError("Card chưa duyệt hoặc chưa có quyền gửi nội dung tới provider")
    endpoint = os.getenv("RECOMMEND_LLM_ENDPOINT", "")
    key = os.getenv("RECOMMEND_LLM_API_KEY", "")
    model = os.getenv("RECOMMEND_LLM_MODEL", "")
    parts = urlsplit(endpoint)
    local_http = parts.scheme == "http" and parts.hostname in ("localhost", "127.0.0.1", "::1")
    if (not (parts.scheme == "https" or local_http) or not parts.hostname
            or parts.username or parts.password or parts.query or parts.fragment
            or not key or not model):
        raise GenerationError("Endpoint phải dùng HTTPS hoặc HTTP trên localhost; đồng thời cần model và API key")
    if parts.path.rstrip("/") == "/v1":
        endpoint = endpoint.rstrip("/") + "/chat/completions"
    messages = build_messages(scenario, cards)
    body = json.dumps({"model": model, "messages": messages, "temperature": 0.3,
                       "response_format": {"type": "json_object"}}, ensure_ascii=False).encode("utf-8")
    req = request.Request(endpoint, data=body, headers={
        "Authorization": f"Bearer {key}", "Content-Type": "application/json"
    }, method="POST")
    try:
        with request.urlopen(req, timeout=30) as response:
            raw = response.read(64_001)
            content_type = getattr(response, "headers", {}).get("Content-Type", "")
        if len(raw) > 64_000:
            raise GenerationError("Phản hồi từ API quá lớn")
        content = _response_content(raw, content_type)
        return validate_output(_address_user(json.loads(content)))
    except error.HTTPError as exc:
        raise GenerationError(f"API trả HTTP {exc.code}; kiểm tra endpoint, model và API key") from None
    except error.URLError as exc:
        raise GenerationError("Không kết nối được tới API") from exc
    except (KeyError, IndexError, ValueError, TypeError) as exc:
        raise GenerationError("API trả dữ liệu không đúng cấu trúc JSON mong đợi") from exc
