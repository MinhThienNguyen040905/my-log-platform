import csv
import json
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from recommend_model.curation import load_sources, prepare


ROOT = Path(__file__).resolve().parents[1]


class CurationTest(unittest.TestCase):
    def test_sources_are_unique_and_pending_export_is_empty(self):
        cards = load_sources(ROOT / "docs")
        self.assertEqual(len(cards), 42)
        self.assertEqual(len({card["id"] for card in cards}), 42)
        with tempfile.TemporaryDirectory() as temp:
            manifest = prepare(ROOT / "docs", Path(temp))
            self.assertEqual(manifest["embedding_ready"], 0)
            self.assertEqual(manifest["blockers"]["not_approved"], 42)
            self.assertEqual((Path(temp) / "embedding_ready.jsonl").read_text(), "")
            with (Path(temp) / "curation_review.csv").open(encoding="utf-8-sig", newline="") as stream:
                rows = list(csv.DictReader(stream))
            self.assertEqual(len(rows), 42)
            self.assertEqual(sum(row["category"] == "self_reflection_content" for row in rows), 19)
            self.assertEqual({row["card_id"] for row in rows if int(row["first_person_template_count"])},
                             {"ref_001", "ref_004", "ref_007"})

    def test_duplicate_in_source_files_is_rejected(self):
        with tempfile.TemporaryDirectory() as temp:
            directory = Path(temp)
            for source in (ROOT / "docs").glob("*.json"):
                (directory / source.name).write_bytes(source.read_bytes())
            path = directory / "psychological_knowledge_additional.json"
            payload = json.loads(path.read_text(encoding="utf-8"))
            payload["knowledge_cards"][1]["id"] = payload["knowledge_cards"][0]["id"]
            path.write_text(json.dumps(payload, ensure_ascii=False), encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "duplicate card ID"):
                load_sources(directory)

    def test_synthetic_approved_card_can_be_exported_once(self):
        with tempfile.TemporaryDirectory() as temp:
            directory = Path(temp) / "docs"
            directory.mkdir()
            for source in (ROOT / "docs").glob("*.json"):
                (directory / source.name).write_bytes(source.read_bytes())
            additional = directory / "psychological_knowledge_additional.json"
            payload = json.loads(additional.read_text(encoding="utf-8"))
            payload["knowledge_cards"][0]["review_status"] = "approved"
            additional.write_text(json.dumps(payload, ensure_ascii=False), encoding="utf-8")
            registry_path = directory / "source_registry.json"
            registry = json.loads(registry_path.read_text(encoding="utf-8"))
            who = registry["sources"]["who_2020"]
            who["citation_review"] = "verified"
            registry_path.write_text(json.dumps(registry), encoding="utf-8")
            decisions_path = directory / "card_decisions.json"
            decisions_path.write_text(json.dumps({"cards": {"who_2020_001": {
                "status": "approved", "citation_verified": True, "original_wording": True,
                "curated_text": "Synthetic note about noticing stress.",
                "topic_code": "STRESS_REFLECTION", "locale": "vi", "reviewed_by": "synthetic-reviewer"
            }}}), encoding="utf-8")
            output = Path(temp) / "output"
            manifest = prepare(directory, output)
            self.assertEqual(manifest["embedding_ready"], 1)
            rows = [json.loads(line) for line in (output / "embedding_ready.jsonl").read_text().splitlines()]
            self.assertEqual([row["card_id"] for row in rows], ["who_2020_001"])
            self.assertEqual(rows[0]["retrieval_text"], "Synthetic note about noticing stress.")


if __name__ == "__main__":
    unittest.main()
