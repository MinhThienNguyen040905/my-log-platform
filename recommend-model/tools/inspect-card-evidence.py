"""Prepare local candidate passages for human source verification; never approves cards."""

import csv
import json
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
OUTPUT = ROOT / "scratch" / "pdf-review"
DOCS = ROOT / "recommend-model" / "docs"


def terms(text: str) -> set[str]:
    return set(re.findall(r"[a-z]{4,}", re.sub(r"\[cite:[^]]+\]", "", text.casefold())))


def best_passage(excerpt: str, page_text: str) -> tuple[float, str]:
    wanted = terms(excerpt)
    words = re.sub(r"\s+", " ", page_text).split()
    if not words or not wanted:
        return 0.0, ""
    window_size = min(len(words), max(45, len(excerpt.split()) * 2))
    best = (0.0, "")
    for start in range(0, len(words), max(1, window_size // 4)):
        passage = " ".join(words[start:start + window_size])
        score = len(wanted & terms(passage)) / len(wanted)
        if score > best[0]:
            best = (score, passage[:700])
    return best


def main() -> None:
    rows = []
    for key, pdf_name in (("tailieu1", "tailieu1.pdf"), ("tailieu2", "tailieuu2.pdf"), ("tailieu3", "tailieu3.pdf")):
        pages = [json.loads(line) for line in (OUTPUT / f"{key}.pages.jsonl").read_text(encoding="utf-8").splitlines()]
        cards = json.loads((DOCS / f"{key}.json").read_text(encoding="utf-8"))["knowledge_cards"]
        for card in cards:
            excerpt = card["source_reference"]["supporting_excerpt"]
            ranked = sorted(((best_passage(excerpt, page["text"])[0], page) for page in pages),
                            key=lambda item: item[0], reverse=True)
            score, page = ranked[0]
            _, passage = best_passage(excerpt, page["text"])
            rows.append({"document_key": key, "card_id": card["id"],
                         "declared_page_start": card["source_reference"]["page_start"],
                         "declared_page_end": card["source_reference"]["page_end"],
                         "candidate_pdf_page": page["pdf_page"],
                         "candidate_page_header": page["text"].split("\n", 1)[0][:100],
                         "passage_term_overlap": f"{score:.2f}", "candidate_passage": passage,
                         "source_checked_by_human": "no", "content_support": "unknown",
                         "printed_page_checked": "unknown", "review_note": ""})
    with (OUTPUT / "card-evidence-review.csv").open("w", encoding="utf-8-sig", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=list(rows[0]))
        writer.writeheader()
        writer.writerows(rows)
    print(f"Prepared {len(rows)} card evidence candidates")


if __name__ == "__main__":
    main()
