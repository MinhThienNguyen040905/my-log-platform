"""Read the five synthetic examples from input.md."""

import re
from dataclasses import dataclass
from pathlib import Path


SCENARIO_CARD_IDS = {
    1: ("who_2020_003", "soq_002"),
    2: ("who_2020_008", "soq_002"),
    3: ("who_2020_010", "soq_006"),
    4: ("who_2020_009", "soq_004"),
    5: ("who_2020_009", "who_2020_010"),
}


@dataclass(frozen=True)
class Scenario:
    number: int
    title: str
    input_text: str
    example_observation: str
    example_questions: tuple[str, str, str]
    card_ids: tuple[str, ...]


def load_scenarios(path: Path) -> tuple[Scenario, ...]:
    content = path.read_text(encoding="utf-8")
    sections = re.findall(r"(?ms)^## ([1-5])\. (.+?)\n(.*?)(?=^## [1-5]\. |\Z)", content)
    if len(sections) != 5:
        raise ValueError("input.md must contain five numbered scenarios")
    scenarios: list[Scenario] = []
    for number_text, title, body in sections:
        number = int(number_text)
        input_match = re.search(r"(?m)^\*\*Câu nhập:\*\* (.+)$", body)
        observation_match = re.search(r"(?m)^\*\*Đoạn gợi mở:\*\* (.+)$", body)
        questions = tuple(re.findall(r"(?m)^[1-3]\. (.+)$", body))
        if not input_match or not observation_match or len(questions) != 3:
            raise ValueError(f"Scenario {number} is incomplete")
        scenarios.append(Scenario(number, title.strip(), input_match.group(1).strip(),
                                  observation_match.group(1).strip(), questions,
                                  SCENARIO_CARD_IDS[number]))
    if tuple(s.number for s in scenarios) != (1, 2, 3, 4, 5):
        raise ValueError("Scenarios must be numbered 1 through 5")
    return tuple(scenarios)
