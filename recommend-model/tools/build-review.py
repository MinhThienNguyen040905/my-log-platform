"""Prepare local per-page text and a cautious JSON-card-to-PDF review table."""

import base64
import csv
import hashlib
import json
import re
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
FILES = (("tailieu1", "tailieu1.pdf"), ("tailieu2", "tailieuu2.pdf"), ("tailieu3", "tailieu3.pdf"))


def normalize(text: str) -> str:
    text = text.replace("\u00ad", "").replace("\r", "")
    text = re.sub(r"(?<=\w)-\n(?=\w)", "", text)
    text = re.sub(r"[ \t]+", " ", text)
    text = re.sub(r"\n{3,}", "\n\n", text)
    return text.strip()


def match_excerpt(excerpt: str, pages: list[dict], start: int, end: int) -> str:
    """Candidate matching only; editorial review must verify source and page numbering."""
    needle = re.sub(r"\s+", " ", excerpt).strip().casefold()
    needle = re.sub(r"\[cite:[^]]+\]", "", needle).strip()
    if not needle or len(needle) < 25:
        return "not_checked"
    haystack = " ".join(p["text"] for p in pages if start <= p["pdf_page"] <= end)
    haystack = re.sub(r"\s+", " ", haystack).casefold()
    if needle in haystack:
        return "exact_in_declared_pdf_pages"
    if needle[:60] in haystack:
        return "prefix_only_in_declared_pdf_pages"
    return "not_found_in_declared_pdf_pages"


def candidate_pages(excerpt: str, pages: list[dict]) -> str:
    """Token overlap suggests pages to inspect; it does not verify a citation."""
    terms = set(re.findall(r"[a-z]{4,}", re.sub(r"\[cite:[^]]+\]", "", excerpt.casefold())))
    if not terms:
        return ""
    scored = []
    for page in pages:
        page_terms = set(re.findall(r"[a-z]{4,}", page["text"].casefold()))
        overlap = len(terms & page_terms) / len(terms)
        if overlap >= 0.55:
            scored.append((overlap, page["pdf_page"]))
    return " | ".join(f"{page}:{score:.2f}" for score, page in sorted(scored, reverse=True)[:3])


def main(output: Path) -> None:
    output.mkdir(parents=True, exist_ok=True)
    rows = []
    summary = []
    for key, pdf_name in FILES:
        source = ROOT / "docs" / "sources" / pdf_name
        card_file = ROOT / "docs" / f"{key}.json"
        card_data = json.loads(card_file.read_text(encoding="utf-8"))
        pages = []
        for line in (output / f"{pdf_name[:-4]}.pages.tsv").read_text(encoding="utf-8").splitlines():
            number, image_count, payload = line.split("\t", 2)
            text = normalize(base64.b64decode(payload).decode("utf-8"))
            pages.append({"pdf_page": int(number), "text": text,
                          "text_sha256": hashlib.sha256(text.encode("utf-8")).hexdigest(),
                          "character_count": len(text), "direct_image_count": int(image_count)})
        if [p["pdf_page"] for p in pages] != list(range(1, len(pages) + 1)):
            raise ValueError(f"Missing or duplicate PDF page in {pdf_name}")
        pdf_sha256 = hashlib.sha256(source.read_bytes()).hexdigest()
        with (output / f"{key}.pages.jsonl").open("w", encoding="utf-8", newline="\n") as stream:
            for page in pages:
                stream.write(json.dumps({"document_key": key, "pdf_sha256": pdf_sha256,
                                         **page}, ensure_ascii=False) + "\n")
        summary.append({"document_key": key, "pdf_file": pdf_name, "pdf_pages": len(pages),
                        "pages_with_text": sum(p["character_count"] >= 50 for p in pages),
                        "pages_under_50_chars": [p["pdf_page"] for p in pages if p["character_count"] < 50],
                        "low_text_pages_with_images": [p["pdf_page"] for p in pages if p["character_count"] < 50 and p["direct_image_count"] > 0],
                        "total_extracted_characters": sum(p["character_count"] for p in pages)})
        for card in card_data["knowledge_cards"]:
            ref = card["source_reference"]
            start, end = ref["page_start"], ref["page_end"]
            rows.append({"document_key": key, "card_id": card["id"], "review_status": card["review_status"],
                         "concept": card["concept"], "concept_vi": card["concept_vi"],
                         "tags": " | ".join(card.get("tags", [])),
                         "limitations": " | ".join(card.get("limitations", [])),
                         "declared_pdf": ref["document_title"], "declared_page_start": start,
                         "declared_page_end": end, "declared_section": ref.get("section", ""),
                         "declared_pages_in_pdf_range": 1 <= start <= end <= len(pages),
                         "excerpt_match": match_excerpt(ref.get("supporting_excerpt", ""), pages, start, end)
                         if 1 <= start <= end <= len(pages) else "page_out_of_range",
                         "candidate_pdf_pages_score": candidate_pages(ref.get("supporting_excerpt", ""), pages),
                         "human_review": "pending"})
    (output / "extraction-summary.json").write_text(json.dumps(summary, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    with (output / "card-review.csv").open("w", encoding="utf-8-sig", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=list(rows[0]))
        writer.writeheader()
        writer.writerows(rows)
    print(json.dumps({"documents": len(summary), "cards": len(rows),
                      "pages": {item["document_key"]: item["pdf_pages"] for item in summary},
                      "matches": {state: sum(r["excerpt_match"] == state for r in rows)
                                  for state in sorted({r["excerpt_match"] for r in rows})}}, ensure_ascii=False))


if __name__ == "__main__":
    if len(sys.argv) != 2:
        raise SystemExit("Usage: build-review.py OUTPUT_DIRECTORY")
    main(Path(sys.argv[1]))
