import io
import json
import sys
import unittest
from contextlib import redirect_stdout
from pathlib import Path
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from recommend_model.cli import main
from recommend_model.generation import GenerationError, _address_user, _response_content, build_messages, generate, provider_use_allowed, validate_output
from recommend_model.knowledge import load_cards
from recommend_model.scenarios import load_scenarios


ROOT = Path(__file__).resolve().parents[1]
SAMPLE_OUTPUT = {
    "observation": (
        "Hôm nay bạn đã cố gắng làm nhiều việc, nhưng cảm giác chưa đủ tốt vẫn ở đó. "
        "Có lẽ bạn đang nhìn kỹ vào những điều chưa hoàn thành. "
        "Bạn có thể thử tìm hiểu điều gì khiến bản thân khó hài lòng lúc này."
    ),
    "questions": [
        "Bạn đang dựa vào điều gì để đánh giá một ngày là đủ tốt?",
        "Có nỗ lực nào hôm nay bạn chưa ghi nhận?",
        "Bạn muốn đối xử với bản thân thế nào khi kết quả chưa như mong đợi?",
    ],
}


class ScenarioFlowTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.scenarios = load_scenarios(ROOT / "input.md")
        cls.cards = load_cards(ROOT / "docs", allow_pending=True)

    def test_five_scenarios_have_local_cards(self):
        self.assertEqual(len(self.scenarios), 5)
        available = {card.id for card in self.cards}
        for scenario in self.scenarios:
            self.assertEqual(len(scenario.example_questions), 3)
            self.assertTrue(set(scenario.card_ids) <= available)

    def test_pending_cards_are_not_live_by_default(self):
        self.assertEqual(load_cards(ROOT / "docs"), ())

    def test_pending_cards_cannot_be_sent_to_provider(self):
        self.assertFalse(provider_use_allowed(self.scenarios[0], self.cards))
        with patch("recommend_model.generation.request.urlopen") as call:
            with self.assertRaises(GenerationError):
                generate(self.scenarios[0], self.cards)
        call.assert_not_called()

    def test_messages_include_system_prompt_selected_input_example_and_json_card(self):
        messages = build_messages(self.scenarios[0], self.cards)
        self.assertEqual(messages[0]["role"], "system")
        self.assertIn("observation", messages[0]["content"])
        data = json.loads(messages[1]["content"])
        self.assertEqual(data["scenario"]["input"], self.scenarios[0].input_text)
        self.assertEqual(data["style_example_from_input_md"]["observation"],
                         self.scenarios[0].example_observation)
        self.assertEqual({card["id"] for card in data["knowledge_cards_from_json"]},
                         set(self.scenarios[0].card_ids))

    def test_invalid_output_is_rejected(self):
        with self.assertRaises(GenerationError):
            validate_output({"observation": "Ngắn.", "questions": ["Một câu hỏi?"]})

    def test_first_person_output_is_rejected(self):
        result = dict(SAMPLE_OUTPUT)
        result["observation"] = result["observation"].replace("bạn", "mình")
        with self.assertRaises(GenerationError):
            validate_output(result)

    def test_question_may_address_user_implicitly(self):
        result = dict(SAMPLE_OUTPUT)
        result["questions"] = list(SAMPLE_OUTPUT["questions"])
        result["questions"][1] = "Có nỗ lực nào hôm nay chưa được ghi nhận?"
        self.assertEqual(validate_output(result), result)

    def test_first_person_provider_text_is_converted_before_validation(self):
        result = {
            "observation": SAMPLE_OUTPUT["observation"].replace("bạn", "mình"),
            "questions": [question.replace("bạn", "mình") for question in SAMPLE_OUTPUT["questions"]],
        }
        self.assertEqual(validate_output(_address_user(result)), SAMPLE_OUTPUT)

    def test_provider_result_is_validated_and_printed_as_json(self):
        raw = {"choices": [{"message": {"content": json.dumps(SAMPLE_OUTPUT, ensure_ascii=False)}}]}
        response = io.BytesIO(json.dumps(raw, ensure_ascii=False).encode("utf-8"))
        config = {
            "RECOMMEND_LLM_ENDPOINT": "https://example.test/v1/chat/completions",
            "RECOMMEND_LLM_MODEL": "test-model",
            "RECOMMEND_LLM_API_KEY": "synthetic-key",
        }
        with patch.dict("os.environ", config), patch("recommend_model.generation.provider_use_allowed", return_value=True), patch("recommend_model.generation.request.urlopen", return_value=response) as call:
            result = generate(self.scenarios[0], self.cards)
        self.assertEqual(result, SAMPLE_OUTPUT)
        body = json.loads(call.call_args.args[0].data)
        self.assertEqual(body["model"], "test-model")
        self.assertEqual(body["messages"][1]["role"], "user")

        output = io.StringIO()
        with patch("builtins.input", side_effect=["1", "q"]), \
             patch("recommend_model.cli.generate", return_value=SAMPLE_OUTPUT), \
             redirect_stdout(output):
            main()
        self.assertIn(json.dumps(SAMPLE_OUTPUT, ensure_ascii=False, indent=2), output.getvalue())

    def test_local_http_provider_is_allowed_but_remote_http_is_rejected(self):
        raw = {"choices": [{"message": {"content": json.dumps(SAMPLE_OUTPUT, ensure_ascii=False)}}]}
        config = {
            "RECOMMEND_LLM_ENDPOINT": "http://127.0.0.1:12345/v1/chat/completions",
            "RECOMMEND_LLM_MODEL": "test-model",
            "RECOMMEND_LLM_API_KEY": "synthetic-key",
        }
        with patch.dict("os.environ", config), patch("recommend_model.generation.provider_use_allowed", return_value=True), \
             patch("recommend_model.generation.request.urlopen", return_value=io.BytesIO(json.dumps(raw).encode())):
            self.assertEqual(generate(self.scenarios[0], self.cards), SAMPLE_OUTPUT)
        config["RECOMMEND_LLM_ENDPOINT"] = "http://example.test/v1/chat/completions"
        with patch.dict("os.environ", config), patch("recommend_model.generation.provider_use_allowed", return_value=True):
            with self.assertRaises(GenerationError):
                generate(self.scenarios[0], self.cards)

    def test_v1_base_endpoint_gets_chat_completions_path(self):
        raw = {"choices": [{"message": {"content": json.dumps(SAMPLE_OUTPUT, ensure_ascii=False)}}]}
        config = {
            "RECOMMEND_LLM_ENDPOINT": "http://127.0.0.1:12345/v1",
            "RECOMMEND_LLM_MODEL": "test-model",
            "RECOMMEND_LLM_API_KEY": "synthetic-key",
        }
        with patch.dict("os.environ", config), patch("recommend_model.generation.provider_use_allowed", return_value=True), \
             patch("recommend_model.generation.request.urlopen", return_value=io.BytesIO(json.dumps(raw).encode())) as call:
            generate(self.scenarios[0], self.cards)
        self.assertEqual(call.call_args.args[0].full_url,
                         "http://127.0.0.1:12345/v1/chat/completions")

    def test_streamed_chat_completion_is_joined(self):
        content = json.dumps(SAMPLE_OUTPUT, ensure_ascii=False)
        chunks = [content[:40], content[40:]]
        stream = "\n\n".join("data: " + json.dumps({"choices": [{"delta": {"content": part}}]})
                              for part in chunks) + "\n\ndata: [DONE]\n"
        self.assertEqual(_response_content(stream.encode(), "text/event-stream"), content)


if __name__ == "__main__":
    unittest.main()
