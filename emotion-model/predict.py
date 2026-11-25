"""Chạy thử model cảm xúc PhoBERT (7 nhãn) trên máy local.

Ví dụ:
  .venv/Scripts/python predict.py "Hôm nay mình vui quá"
  .venv/Scripts/python predict.py            # chế độ gõ tương tác
  .venv/Scripts/python predict.py --no-segment "..."   # bỏ tách từ

PhoBERT thường được train trên văn bản đã tách từ (vd "học_sinh").
Mặc định script tách từ bằng underthesea; dùng --no-segment để so sánh
nếu notebook Kaggle không tách từ khi train.
Chỉ dùng câu synthetic, không dán nội dung nhật ký thật.
"""

import argparse
import json
import sys
from pathlib import Path

import torch
from transformers import AutoModelForSequenceClassification, AutoTokenizer

MODEL_DIR = Path(__file__).resolve().parent / "emotion_model"


def load(model_dir: Path):
    tokenizer = AutoTokenizer.from_pretrained(model_dir)
    model = AutoModelForSequenceClassification.from_pretrained(model_dir)
    model.eval()
    labels_file = model_dir / "labels.json"
    if labels_file.exists():
        labels = json.loads(labels_file.read_text(encoding="utf-8"))
    else:
        labels = [model.config.id2label[i] for i in range(model.config.num_labels)]
    return tokenizer, model, labels


def segment(text: str) -> str:
    from underthesea import word_tokenize

    return word_tokenize(text, format="text")


@torch.no_grad()
def predict(text, tokenizer, model, labels, use_segment=True, top_k=3):
    prepared = segment(text) if use_segment else text
    enc = tokenizer(prepared, return_tensors="pt", truncation=True, max_length=256)
    probs = torch.softmax(model(**enc).logits[0], dim=-1)
    ranked = sorted(zip(labels, probs.tolist()), key=lambda x: x[1], reverse=True)
    return prepared, ranked[:top_k]


def show(text, args, tokenizer, model, labels):
    prepared, ranked = predict(text, tokenizer, model, labels, not args.no_segment, args.top_k)
    print(f"\n> {text}")
    if prepared != text:
        print(f"  (đã tách từ: {prepared})")
    for label, p in ranked:
        print(f"  {label:<10} {p:6.1%}  {'#' * int(p * 30)}")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("texts", nargs="*")
    parser.add_argument("--model-dir", type=Path, default=MODEL_DIR)
    parser.add_argument("--no-segment", action="store_true")
    parser.add_argument("--top-k", type=int, default=3)
    args = parser.parse_args()

    sys.stdout.reconfigure(encoding="utf-8")
    tokenizer, model, labels = load(args.model_dir)

    if args.texts:
        for t in args.texts:
            show(t, args, tokenizer, model, labels)
        return

    print("Gõ một câu tiếng Việt (Enter trống để thoát).")
    while True:
        try:
            t = input("\ncâu> ").strip()
        except (EOFError, KeyboardInterrupt):
            break
        if not t:
            break
        show(t, args, tokenizer, model, labels)


if __name__ == "__main__":
    main()
