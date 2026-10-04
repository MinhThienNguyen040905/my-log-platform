"""Train a review-only baseline from separately approved, labeled CSV files.

The holdout CSV must be collected independently; this command never exports app data.
"""

import argparse
import csv
import hashlib
import json
import re
import sys
from collections import Counter
from pathlib import Path

import joblib
import sklearn
from sklearn.calibration import CalibratedClassifierCV
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import classification_report, confusion_matrix
from sklearn.pipeline import make_pipeline

LABELS = ("NORMAL", "LOW", "MODERATE", "HIGH", "CRITICAL")


def read_labeled(path: Path):
    with path.open(encoding="utf-8-sig", newline="") as stream:
        reader = csv.DictReader(stream)
        if not {"text", "label"}.issubset(reader.fieldnames or []):
            raise ValueError("CSV must contain text and label columns")
        rows = [(row["text"].strip(), row["label"].strip()) for row in reader]
    if not rows or any(not text or label not in LABELS for text, label in rows):
        raise ValueError("Empty text or invalid risk label")
    return rows


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--train", type=Path, required=True)
    parser.add_argument("--holdout", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--version", required=True)
    args = parser.parse_args()
    if not re.fullmatch(r"[A-Za-z0-9._:-]{1,80}", args.version):
        raise ValueError("Invalid model version")
    train = read_labeled(args.train)
    holdout = read_labeled(args.holdout)
    counts = Counter(label for _, label in train)
    if any(counts[label] < 20 for label in LABELS):
        raise ValueError("Need at least 20 reviewed training examples per class")
    if {text for text, _ in train} & {text for text, _ in holdout}:
        raise ValueError("Train and holdout contain duplicate text")

    estimator = make_pipeline(
        TfidfVectorizer(analyzer="char", ngram_range=(2, 5), min_df=2,
                        max_features=200_000, lowercase=True),
        LogisticRegression(class_weight="balanced", max_iter=2000),
    )
    model = CalibratedClassifierCV(estimator, cv=3)
    model.fit([text for text, _ in train], [label for _, label in train])
    actual = [label for _, label in holdout]
    predicted = model.predict([text for text, _ in holdout])
    report = classification_report(actual, predicted, labels=LABELS,
                                   output_dict=True, zero_division=0)
    matrix = confusion_matrix(actual, predicted, labels=LABELS).tolist()

    args.output.mkdir(parents=True, exist_ok=False)
    artifact = args.output / "model.joblib"
    joblib.dump(model, artifact)
    metadata = {
        "version": args.version,
        "labels": LABELS,
        "train_count": len(train),
        "holdout_count": len(holdout),
        "holdout_report": report,
        "holdout_confusion_matrix": matrix,
        "train_sha256": hashlib.sha256(args.train.read_bytes()).hexdigest(),
        "holdout_sha256": hashlib.sha256(args.holdout.read_bytes()).hexdigest(),
        "python_version": sys.version.split()[0],
        "scikit_learn_version": sklearn.__version__,
        "artifact_sha256": hashlib.sha256(artifact.read_bytes()).hexdigest(),
        "approved": False,
    }
    (args.output / "manifest.json").write_text(
        json.dumps(metadata, ensure_ascii=False, indent=2), encoding="utf-8")
    print("Model trained; review manifest.json before any deployment")


if __name__ == "__main__":
    main()
