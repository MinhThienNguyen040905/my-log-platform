"""Create local heading and provider-rights review files for docs ingestion."""

import csv
import json
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DOCS = ROOT / "docs"
OUTPUT = ROOT.parent / "scratch" / "recommend-model-all-docs"


def write_csv(path, rows, fields):
    with path.open("w", encoding="utf-8-sig", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=fields)
        writer.writeheader()
        writer.writerows(rows)


def main():
    chunks_path = OUTPUT / "chunks.jsonl"
    chunks = [json.loads(line) for line in chunks_path.read_text(encoding="utf-8").splitlines()]
    section_map = {}
    for chunk in chunks:
        meta = chunk["metadata"]
        if meta.get("kind") != "pdf_section":
            continue
        key = (chunk["source_file"], meta["record_id"])
        if key not in section_map:
            section_map[key] = {
                "pdf_file": chunk["source_file"],
                "section_id": meta["record_id"],
                "heading": meta.get("heading", ""),
                "start_page": meta.get("start_page"),
                "end_page": meta.get("end_page"),
                "chunk_count": 0,
                "sample": chunk["text"][:260],
            }
        section_map[key]["chunk_count"] += 1
    headings = sorted(section_map.values(), key=lambda row: (row["pdf_file"], row["start_page"] or 0))
    write_csv(OUTPUT / "pdf-heading-review.csv", headings, list(headings[0]))

    registry = json.loads((DOCS / "source_registry.json").read_text(encoding="utf-8"))["sources"]
    id_to_source = {"soq_": "soq", "nqg_": "nqg", "ref_": "ref",
                    "who_2020_": "who_2020", "thought_record_user_": "thought_record_user"}
    card_counts = Counter()
    for path in DOCS.glob("*.json"):
        data = json.loads(path.read_text(encoding="utf-8"))
        if path.name == "psychological_knowledge.json":
            continue  # exact duplicate aggregate; not part of the ingestion feed
        for card in data.get("knowledge_cards", []):
            card_id = card.get("id", "")
            key = next((key for prefix, key in id_to_source.items() if card_id.startswith(prefix)), None)
            if key:
                card_counts[key] += 1

    rights_rows = []
    for key, item in registry.items():
        rights = item.get("rights", {})
        rights_rows.append({
            "file": item.get("file", ""),
            "source_key": key,
            "content_kind": item.get("source_kind", ""),
            "card_count": card_counts.get(key, 0),
            "citation_review": item.get("citation_review", "unknown"),
            "store_in_db": rights.get("store_in_db", "unknown"),
            "send_to_embedding_provider": rights.get("send_to_embedding_provider", "unknown"),
            "ingestion_decision": "BLOCKED_UNTIL_RIGHTS_CONFIRMED",
            "reason": "Embedding-provider permission is not approved in source_registry.json.",
        })
    for pdf in sorted((DOCS / "sources").glob("*.pdf")):
        rights_rows.append({
            "file": f"sources/{pdf.name}", "source_key": "", "content_kind": "PDF book/document",
            "card_count": 0, "citation_review": "not_registered", "store_in_db": "unknown",
            "send_to_embedding_provider": "unknown", "ingestion_decision": "BLOCKED_UNTIL_RIGHTS_CONFIRMED",
            "reason": "PDF has no approved embedding-provider right recorded; check its license/source.",
        })
    write_csv(OUTPUT / "embedding-rights-review.csv", rights_rows, list(rights_rows[0]))

    summary = {
        "heading_review_file": str(OUTPUT / "pdf-heading-review.csv"),
        "heading_sections_to_review": len(headings),
        "rights_review_file": str(OUTPUT / "embedding-rights-review.csv"),
        "sources_blocked_pending_permission": len(rights_rows),
        "embedding_provider_calls_made": 0,
        "database_writes_made": 0,
    }
    (OUTPUT / "ingestion-review-summary.json").write_text(
        json.dumps(summary, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(summary, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
