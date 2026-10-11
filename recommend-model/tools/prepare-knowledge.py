"""Create review artifacts and an approval-gated JSONL feed under scratch/."""

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from recommend_model.curation import prepare  # noqa: E402


if __name__ == "__main__":
    result = prepare(ROOT / "docs", ROOT.parent / "scratch" / "knowledge-preparation")
    print(json.dumps(result, ensure_ascii=False, indent=2))
