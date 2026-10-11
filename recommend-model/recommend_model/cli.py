"""Choose one of five synthetic scenarios and print the LLM's validated JSON."""

import json
from pathlib import Path

from .generation import GenerationError, generate
from .knowledge import load_cards
from .scenarios import load_scenarios


ROOT = Path(__file__).resolve().parents[1]


def main() -> None:
    scenarios = load_scenarios(ROOT / "input.md")
    cards = load_cards(ROOT / "docs", allow_pending=True)
    print("Chọn một kịch bản giả định để gọi LLM:\n")
    for scenario in scenarios:
        print(f"{scenario.number}. {scenario.title}")
    print("\nGõ q để thoát.")
    while True:
        try:
            choice = input("\nKịch bản (1-5): ").strip().casefold()
        except (EOFError, KeyboardInterrupt):
            print("\nĐã dừng.")
            return
        if choice in ("q", "quit", "thoát", "thoat"):
            print("Đã dừng.")
            return
        if choice not in ("1", "2", "3", "4", "5"):
            print("Hãy chọn số từ 1 đến 5.")
            continue
        scenario = scenarios[int(choice) - 1]
        print(f"Đang gọi LLM cho: {scenario.title}...")
        try:
            result = generate(scenario, cards)
        except GenerationError as exc:
            print(f"Lỗi: {exc}")
            continue
        print(json.dumps(result, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
