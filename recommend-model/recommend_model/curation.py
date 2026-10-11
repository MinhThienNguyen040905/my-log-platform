"""Validate source cards and export reviewed, independently written knowledge."""

import csv
import hashlib
import json
import re
from pathlib import Path


SOURCE_FILES = (
    "socratic_question_generation.json",
    "neural_question_generation_survey.json",
    "automatic_reflection_generation.json",
    "psychological_knowledge_additional.json",
)
RESEARCH_METHOD_PREFIXES = ("soq_", "nqg_", "ref_")
CONTENT_PREFIXES = ("who_2020_", "thought_record_user_")


def load_sources(directory: Path) -> tuple[dict, ...]:
    cards = []
    seen = set()
    for name in SOURCE_FILES:
        data = json.loads((directory / name).read_text(encoding="utf-8"))
        if not isinstance(data.get("knowledge_cards"), list):
            raise ValueError(f"Missing knowledge_cards in {name}")
        for card in data["knowledge_cards"]:
            card_id = card.get("id")
            if not isinstance(card_id, str) or not card_id or card_id in seen:
                raise ValueError(f"Missing or duplicate card ID: {card_id}")
            if card.get("review_status") not in ("pending", "approved", "rejected"):
                raise ValueError(f"Invalid review status: {card_id}")
            if not isinstance(card.get("source_reference"), dict):
                raise ValueError(f"Missing source reference: {card_id}")
            seen.add(card_id)
            cards.append({**card, "source_file": name})
    aggregate = directory / "psychological_knowledge.json"
    if aggregate.exists():
        copies = json.loads(aggregate.read_text(encoding="utf-8"))["knowledge_cards"]
        original = {card["id"]: {k: v for k, v in card.items() if k != "source_file"}
                    for card in cards if card["id"].startswith(RESEARCH_METHOD_PREFIXES)}
        if len(copies) != len(original) or any(copy != original.get(copy.get("id")) for copy in copies):
            raise ValueError("psychological_knowledge.json differs from its three source files")
    return tuple(cards)


def source_key(card_id: str) -> str:
    for prefix in (*RESEARCH_METHOD_PREFIXES, *CONTENT_PREFIXES):
        if card_id.startswith(prefix):
            return prefix.rstrip("_")
    raise ValueError(f"Unknown card family: {card_id}")


def category(card_id: str) -> str:
    if card_id.startswith(CONTENT_PREFIXES):
        return "self_reflection_content"
    if card_id in {"soq_007", "soq_008", "nqg_007", "nqg_008", "ref_002", "ref_004", "ref_005", "ref_006"} or card_id.startswith("nqg_"):
        return "engineering_evaluation"
    return "generation_guidance"


def prepare(directory: Path, output: Path) -> dict:
    cards = load_sources(directory)
    registry = json.loads((directory / "source_registry.json").read_text(encoding="utf-8"))["sources"]
    decisions = json.loads((directory / "card_decisions.json").read_text(encoding="utf-8"))["cards"]
    known = {card["id"] for card in cards}
    if set(decisions) - known:
        raise ValueError("Decision references unknown card IDs")
    output.mkdir(parents=True, exist_ok=True)
    review = []
    ready = []
    blocked = {}
    for card in cards:
        card_id = card["id"]
        key = source_key(card_id)
        source = registry[key]
        if source["file"] != card["source_file"]:
            raise ValueError(f"Wrong source file for {card_id}")
        group = category(card_id)
        decision = decisions.get(card_id, {})
        suggested_text = f"{card['concept_vi'].strip()}. {card['definition'].strip()}"
        reasons = []
        if group != "self_reflection_content":
            reasons.append("internal_method_card")
        if card["review_status"] != "approved" or decision.get("status") != "approved":
            reasons.append("not_approved")
        if source["citation_review"] != "verified" or decision.get("citation_verified") is not True:
            reasons.append("citation_unverified")
        if not decision.get("curated_text", "").strip():
            reasons.append("curated_text_missing")
        if not decision.get("reviewed_by"):
            reasons.append("reviewer_missing")
        if not source.get("url", ""):
            reasons.append("source_url_missing")
        # This export contains only independently authored text, not source excerpts.
        # A reviewer must explicitly confirm that distinction for every card.
        if decision.get("original_wording") is not True:
            reasons.append("original_wording_unconfirmed")
        review.append({"card_id": card_id, "source_file": card["source_file"],
                       "source_key": key, "category": group, "review_status": card["review_status"],
                       "evidence_type": card["evidence_type"],
                       "generated_signal_count": sum(x.get("origin") == "generated_example" for x in card["observable_signals"]),
                       "first_person_template_count": sum(bool(re.search(r"^\s*(mình|tôi)\b", q["question"], re.IGNORECASE))
                                                          for q in card["question_templates"]),
                       "suggested_text": suggested_text, "blockers": " | ".join(reasons)})
        if reasons:
            for reason in reasons:
                blocked[reason] = blocked.get(reason, 0) + 1
            continue
        content = decision["curated_text"].strip()
        if len(content) > 800:
            raise ValueError(f"Curated text too long for one chunk: {card_id}")
        topic_code = decision.get("topic_code")
        locale = decision.get("locale")
        if not isinstance(topic_code, str) or not re.fullmatch(r"[A-Z0-9_]{1,48}", topic_code):
            raise ValueError(f"Invalid topic code: {card_id}")
        if not isinstance(locale, str) or not re.fullmatch(r"[a-z]{2}(?:-[A-Z]{2})?", locale):
            raise ValueError(f"Invalid locale: {card_id}")
        ready.append({"card_id": card_id, "source_key": key, "source_url": source["url"],
                      "source_title": card["source_reference"]["document_title"],
                      "source_page_start": card["source_reference"].get("page_start"),
                      "source_page_end": card["source_reference"].get("page_end"),
                      "locale": locale, "topic_code": topic_code,
                      "retrieval_text": content,
                      "content_sha256": hashlib.sha256(content.encode("utf-8")).hexdigest(),
                      "reviewed_by": decision["reviewed_by"]})
    with (output / "curation_review.csv").open("w", encoding="utf-8-sig", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=list(review[0]))
        writer.writeheader()
        writer.writerows(review)
    with (output / "embedding_ready.jsonl").open("w", encoding="utf-8") as stream:
        for item in ready:
            stream.write(json.dumps(item, ensure_ascii=False) + "\n")
    manifest = {"source_cards": len(cards), "unique_cards": len(known),
                "review_rows": len(review), "embedding_ready": len(ready), "blockers": blocked,
                "note": "No vector is generated or written to PostgreSQL by this command."}
    (output / "preparation_manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return manifest
