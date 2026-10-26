"""Build a deterministic chunk feed from every unique docs JSON, Markdown, and PDF page."""

import hashlib
import json
import re
import sys
import base64
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DOCS = ROOT / "docs"
PDF_PAGES = ROOT.parent / "scratch" / "recommend-model-pdf-pages"
OUTPUT = ROOT.parent / "scratch" / "recommend-model-all-docs"
MAX_CHARS = 1200
OVERLAP = 120
PDF_MAX_CHARS = 2400
PDF_OVERLAP = 240


def flatten(value):
    if isinstance(value, dict):
        return "\n".join(f"{key}: {flatten(child)}" for key, child in value.items())
    if isinstance(value, list):
        return "\n".join(flatten(child) for child in value)
    if value is None:
        return ""
    return str(value)


def chunks(text, max_chars=MAX_CHARS, overlap=OVERLAP):
    text = re.sub(r"\s+", " ", text).strip()
    if not text:
        return []
    result = []
    start = 0
    while start < len(text):
        end = min(start + max_chars, len(text))
        if end < len(text):
            boundary = max(text.rfind(". ", start, end), text.rfind("; ", start, end),
                           text.rfind(" ", start + max_chars // 2, end))
            if boundary > start:
                end = boundary + 1
        part = text[start:end].strip()
        if part:
            result.append(part)
        if end >= len(text):
            break
        start = max(start + 1, end - overlap)
    return result


def digest(text):
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def is_pdf_heading(line):
    """Conservative heading heuristic for text-only PDF extraction."""
    line = re.sub(r"\s+", " ", line).strip(" \t\u2022")
    if not line or len(line) > 120 or len(line.split()) > 14:
        return False
    if re.search(r"\b(published by|printed in|copyright|all rights reserved|cataloging|new york|london|edition|press)\b", line, re.I):
        return False
    structural = re.match(r"^(chapter|part|appendix)\s+([0-9ivx]+|[a-z])\b(.*)$", line, re.I)
    if structural:
        remainder = structural.group(3).strip()
        if remainder.startswith((")", "(", ",")) or re.search(r"[.!?].+", remainder):
            return False
        remainder = remainder.lstrip(".:–—- ")
        if remainder and not remainder[:1].isupper():
            return False
        return True
    if re.search(r"\d", line) or re.match(r"^(?:[A-Z]\s+){3,}", line):
        return False
    # PDF text extraction loses font/style metadata; uppercase standalone lines
    # are the most reliable remaining heading signal.
    letters = [c for c in line if c.isalpha()]
    if len(letters) >= 4 and sum(c.isupper() for c in letters) / len(letters) > 0.88:
        return True
    # Also recognize short standalone title-case headings, but not running text.
    if not re.match(r"^[A-Z]", line) or not re.fullmatch(r"[A-Za-zÀ-ỹ][A-Za-zÀ-ỹ'’\-]*(?:\s+[A-Za-zÀ-ỹ][A-Za-zÀ-ỹ'’\-]*){2,7}\??", line):
        return False
    title = line[:-1] if line.endswith("?") else line
    words = title.split()
    minor = {"a", "an", "and", "as", "at", "but", "by", "for", "from", "in",
             "into", "of", "on", "or", "the", "to", "with"}
    major = [w for w in words if w.casefold() not in minor]
    return (3 <= len(words) <= 8 and len(major) >= 2
            and all(w[:1].isupper() for w in major if w[:1].isalpha()))


def pdf_sections(page_rows):
    """Group consecutive PDF pages by detected headings, retaining page ranges."""
    toc_pages = set()
    for index, (page, text) in enumerate(page_rows):
        if page > 60 or not re.search(r"\bcontents\b", text, re.I):
            continue
        toc_pages.add(page)
        for next_page, next_text in page_rows[index + 1:index + 8]:
            lines = [line.strip() for line in next_text.splitlines() if line.strip()]
            if not lines:
                break
            numbered = sum(bool(re.search(r"\s\d{1,3}\s*$", line)) for line in lines)
            if numbered / len(lines) < 0.12:
                break
            toc_pages.add(next_page)
    line_counts = {}
    for _, text in page_rows:
        for raw in text.splitlines():
            line = re.sub(r"\s+", " ", raw).strip()
            if line:
                line_counts[line.casefold()] = line_counts.get(line.casefold(), 0) + 1
    sections = []
    current = {"heading": "Opening / unheaded content", "start_page": None,
               "end_page": None, "parts": []}
    for page, page_text in page_rows:
        if page in toc_pages:
            if current["parts"] and current["heading"] != "Table of Contents":
                sections.append(current)
                current = {"heading": "Table of Contents", "start_page": page,
                           "end_page": page, "parts": []}
            elif current["start_page"] is None:
                current["heading"] = "Table of Contents"
                current["start_page"] = page
            current["end_page"] = page
            current["parts"].extend(line.strip() for line in page_text.splitlines() if line.strip())
            continue
        if current["heading"] == "Table of Contents" and current["parts"]:
            sections.append(current)
            current = {"heading": "Opening / unheaded content", "start_page": None,
                       "end_page": None, "parts": []}
        for raw in page_text.splitlines():
            line = re.sub(r"\s+", " ", raw).strip()
            if not line:
                continue
            # Printed page headers often look like "86 INTRODUCTION" and
            # must not become section boundaries.
            if re.match(r"^\d+\s+[A-Z]", line):
                continue
            if line_counts.get(line.casefold(), 0) <= 3 and is_pdf_heading(line):
                if current["parts"]:
                    sections.append(current)
                current = {"heading": line, "start_page": page,
                           "end_page": page, "parts": []}
                continue
            if current["start_page"] is None:
                current["start_page"] = page
            current["end_page"] = page
            current["parts"].append(line)
    if current["parts"]:
        sections.append(current)
    return sections


def pdf_chunks(text):
    """Split an unusually long heading section without crossing section boundaries."""
    return chunks(text, PDF_MAX_CHARS, PDF_OVERLAP)


def emit(records, source, text, metadata):
    for index, part in enumerate(chunks(text)):
        source_id = digest(f"{source}\0{metadata.get('record_id', '')}\0{metadata.get('page', '')}")
        records.append({
            "chunk_id": digest(f"{source_id}\0{index}\0{part}"),
            "source_file": source,
            "chunk_index": index,
            "text": part,
            "metadata": metadata,
        })


def main():
    OUTPUT.mkdir(parents=True, exist_ok=True)
    records = []
    seen_cards = set()
    duplicate_cards = 0
    aggregate_cards_excluded = 0
    source_file_count = 0
    empty_pdf_pages = 0
    for path in sorted(DOCS.rglob("*")):
        if not path.is_file():
            continue
        source = path.relative_to(DOCS).as_posix()
        source_file_count += 1
        if path.suffix.lower() == ".json":
            data = json.loads(path.read_text(encoding="utf-8"))
            if isinstance(data, dict) and isinstance(data.get("knowledge_cards"), list):
                meta = {key: value for key, value in data.items() if key != "knowledge_cards"}
                if meta:
                    emit(records, source, flatten(meta), {"kind": "document_metadata"})
                if path.name == "psychological_knowledge.json":
                    aggregate_cards_excluded += len(data["knowledge_cards"])
                    source_cards = []
                else:
                    source_cards = data["knowledge_cards"]
                for card in source_cards:
                    card_id = card.get("id")
                    if card_id in seen_cards:
                        duplicate_cards += 1
                        continue
                    seen_cards.add(card_id)
                    metadata = {
                        "kind": "knowledge_card",
                        "record_id": card_id,
                        "review_status": card.get("review_status"),
                        "tags": card.get("tags", []),
                        "source_reference": card.get("source_reference", {}),
                    }
                    emit(records, source, flatten(card), metadata)
            else:
                emit(records, source, flatten(data), {"kind": "metadata_document"})
        elif path.suffix.lower() == ".md":
            emit(records, source, path.read_text(encoding="utf-8"), {"kind": "markdown"})
        elif path.suffix.lower() == ".pdf":
            extracted = PDF_PAGES / f"{path.stem}.pages.tsv"
            if not extracted.is_file():
                raise FileNotFoundError(f"Missing PDF page extraction for {source}")
            page_rows = []
            for line in extracted.read_text(encoding="utf-8").splitlines():
                page, image_count, payload = line.split("\t", 2)
                page_text = base64.b64decode(payload).decode("utf-8")
                if not page_text.strip():
                    empty_pdf_pages += 1
                    continue
                page_rows.append((int(page), page_text))
            for section_index, section in enumerate(pdf_sections(page_rows)):
                section_text = "\n".join(section["parts"])
                for chunk_index, part in enumerate(pdf_chunks(section_text)):
                    section_id = f"{path.stem}:section:{section_index}"
                    records.append({
                        "chunk_id": digest(f"{source}\0{section_id}\0{chunk_index}\0{part}"),
                        "source_file": source,
                        "chunk_index": chunk_index,
                        "text": part,
                        "metadata": {
                            "kind": "pdf_section",
                            "record_id": section_id,
                            "heading": section["heading"],
                            "start_page": section["start_page"],
                            "end_page": section["end_page"],
                        },
                    })

    output_file = OUTPUT / "chunks.jsonl"
    tsv_file = OUTPUT / "chunks.tsv"
    with output_file.open("w", encoding="utf-8", newline="\n") as stream:
        for record in records:
            stream.write(json.dumps(record, ensure_ascii=False) + "\n")
    with tsv_file.open("w", encoding="ascii", newline="\n") as stream:
        for record in records:
            metadata = json.dumps(record["metadata"], ensure_ascii=False, separators=(",", ":"))
            fields = (record["chunk_id"], record["source_file"], metadata, record["text"])
            stream.write("\t".join(base64.b64encode(value.encode("utf-8")).decode("ascii") for value in fields) + "\n")
    manifest = {
        "source_files": source_file_count,
        "unique_cards": len(seen_cards),
        "aggregate_duplicate_cards_excluded": aggregate_cards_excluded,
        "duplicate_card_records_skipped": duplicate_cards,
        "chunks": len(records),
        "empty_pdf_pages_without_extractable_text": empty_pdf_pages,
        "chunk_max_chars": MAX_CHARS,
        "chunk_overlap_chars": OVERLAP,
        "pdf_chunking": "detected_heading_sections_with_page_ranges",
        "pdf_chunk_max_chars": PDF_MAX_CHARS,
        "pdf_chunk_overlap_chars": PDF_OVERLAP,
        "output": str(output_file),
        "database_import_feed": str(tsv_file),
        "review_status_is_metadata_only": True,
        "vectors_created": 0,
        "database_written": False,
    }
    (OUTPUT / "manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(manifest, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
