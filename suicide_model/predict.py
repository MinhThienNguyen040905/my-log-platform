"""Chạy thử classifier nguy cơ tự hại (nhị phân) trên máy local.

Đọc suicide_model/deploy_config.json:
  - vi: ensemble trung bình prob của các fold PhoBERT, ngưỡng mặc định 0.1
  - en: RoBERTa, ngưỡng mặc định 0.24

Ví dụ:
  python predict.py "câu tiếng Việt"            # mặc định --lang vi
  python predict.py --lang en "an english sentence"
  python predict.py                              # chế độ gõ tương tác
  python predict.py --verify suicide_model/pred_vi_test.csv --limit 20
  python predict.py --no-segment "..."           # bỏ tách từ (sẽ lệch so với lúc train)
      # so prob với kết quả Kaggle để kiểm tra pipeline local khớp lúc train

Đây là bản prototype để thử nghiệm, chưa được duyệt để xử lý dữ liệu người
dùng. Chỉ dùng câu synthetic, không dán nội dung nhật ký thật.
"""

import argparse
import csv
import json
import sys
from pathlib import Path

import torch
from transformers import AutoModelForSequenceClassification, AutoTokenizer

ROOT = Path(__file__).resolve().parent / "suicide_model"


class Classifier:
    def __init__(self, lang: str, root: Path, segment: bool):
        cfg = json.loads((root / "deploy_config.json").read_text(encoding="utf-8"))[lang]
        dirs = cfg["models"] if "models" in cfg else [cfg["model"]]
        self.threshold = cfg["threshold"]
        self.max_len = cfg["max_len"]
        self.segment = segment and lang == "vi"
        self.members = []
        for d in dirs:
            tok = AutoTokenizer.from_pretrained(root / d)
            model = AutoModelForSequenceClassification.from_pretrained(root / d).eval()
            self.members.append((tok, model))

    def prepare(self, text: str) -> str:
        if not self.segment:
            return text
        from underthesea import word_tokenize

        return word_tokenize(text, format="text")

    @torch.no_grad()
    def prob(self, text: str) -> float:
        prepared = self.prepare(text)
        probs = []
        for tok, model in self.members:
            enc = tok(prepared, return_tensors="pt", truncation=True, max_length=self.max_len)
            probs.append(torch.softmax(model(**enc).logits[0], dim=-1)[1].item())
        return sum(probs) / len(probs)


def show(clf: Classifier, text: str, threshold: float):
    p = clf.prob(text)
    flag = "NGUY CƠ" if p >= threshold else "không"
    print(f"\n> {text}\n  prob_1 = {p:.3f}  (ngưỡng {threshold})  ->  {flag}")


def verify(clf: Classifier, csv_path: Path, limit: int):
    """So prob local với cột prob_1 Kaggle; không in nội dung câu."""
    with csv_path.open(encoding="utf-8-sig") as f:
        rows = list(csv.DictReader(f))[:limit]
    diffs = [abs(clf.prob(r["text"]) - float(r["prob_1"])) for r in rows]
    print(f"{len(rows)} dòng | lệch prob trung bình {sum(diffs) / len(diffs):.4f} | lớn nhất {max(diffs):.4f}")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("texts", nargs="*")
    ap.add_argument("--lang", choices=["vi", "en"], default="vi")
    ap.add_argument("--root", type=Path, default=ROOT)
    ap.add_argument("--threshold", type=float, help="ghi đè ngưỡng trong deploy_config")
    ap.add_argument("--no-segment", action="store_true",
                    help="bỏ tách từ tiếng Việt (model vi được train trên văn bản đã tách từ)")
    ap.add_argument("--verify", type=Path, help="file pred_*.csv từ Kaggle")
    ap.add_argument("--limit", type=int, default=20)
    args = ap.parse_args()

    sys.stdout.reconfigure(encoding="utf-8")
    clf = Classifier(args.lang, args.root, not args.no_segment)
    threshold = args.threshold if args.threshold is not None else clf.threshold

    if args.verify:
        verify(clf, args.verify, args.limit)
        return
    if args.texts:
        for t in args.texts:
            show(clf, t, threshold)
        return

    print(f"[{args.lang}] Gõ một câu (Enter trống để thoát).")
    while True:
        try:
            t = input("\ncâu> ").strip()
        except (EOFError, KeyboardInterrupt):
            break
        if not t:
            break
        show(clf, t, threshold)


if __name__ == "__main__":
    main()
