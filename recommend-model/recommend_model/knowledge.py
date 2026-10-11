"""Load local knowledge cards for a synthetic-data prototype."""

import json
from dataclasses import dataclass
from pathlib import Path

from .curation import SOURCE_FILES


@dataclass(frozen=True)
class Card:
    id: str
    framework: str
    concept: str
    concept_vi: str
    definition: str
    reflection_goal: str
    contexts: tuple[str, ...]
    limitations: tuple[str, ...]
    tags: tuple[str, ...]
    questions: tuple[str, ...]
    review_status: str
    source_file: str


def load_cards(directory: Path, *, allow_pending: bool = False) -> tuple[Card, ...]:
    paths = [directory / name for name in SOURCE_FILES]
    if any(not path.is_file() for path in paths):
        raise ValueError("Missing one or more source knowledge JSON files")
    cards: list[Card] = []
    seen: set[str] = set()
    for path in paths:
        data = json.loads(path.read_text(encoding="utf-8"))
        if not isinstance(data.get("knowledge_cards"), list):
            raise ValueError(f"Missing knowledge_cards in {path.name}")
        for raw in data["knowledge_cards"]:
            card_id = raw["id"]
            if not isinstance(card_id, str) or not card_id or card_id in seen:
                raise ValueError("Missing or duplicate card ID")
            seen.add(card_id)
            status = raw["review_status"]
            if status not in ("pending", "approved", "rejected"):
                raise ValueError(f"Invalid review status for {card_id}")
            questions = tuple(q["question"].strip() for q in raw["question_templates"])
            if len(questions) < 2 or any(not q for q in questions):
                raise ValueError(f"Missing question templates for {card_id}")
            card = Card(
                id=card_id,
                framework=raw["framework"].strip(),
                concept=raw["concept"].strip(),
                concept_vi=raw["concept_vi"].strip(),
                definition=raw["definition"].strip(),
                reflection_goal=raw["reflection_goal"].strip(),
                contexts=tuple(raw["application_contexts"]),
                limitations=tuple(raw["limitations"]),
                tags=tuple(raw["tags"]),
                questions=questions,
                review_status=status,
                source_file=path.name,
            )
            if status == "approved" or allow_pending and status == "pending":
                cards.append(card)
    return tuple(cards)
