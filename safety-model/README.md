# Safety classifier training and inference

Detailed model scope, dataset, evaluation and release criteria: [SAFETY_CLASSIFIER_MODEL_PLAN.md](../backend/docs/SAFETY_CLASSIFIER_MODEL_PLAN.md).

This separate Python component is a **trainable baseline**, not an approved safety model.
No training data or model artifact is committed to the repository. Use only licensed/synthetic
or explicitly consented and reviewed data. `AI_PROCESSING` is not `MODEL_TRAINING` consent.

Create two independently collected UTF-8 CSV files with `text,label` columns. Allowed labels:
`NORMAL`, `LOW`, `MODERATE`, `HIGH`, `CRITICAL`. Run:

```powershell
python -m venv .venv
.venv/Scripts/python -m pip install -r requirements.txt
.venv/Scripts/python train.py --train train.csv --holdout holdout.csv --output artifacts/v1 --version v1
```

On MSYS Python, use `.venv/bin/python` in place of `.venv/Scripts/python`.
The provided Dockerfile uses CPython 3.12/Linux binary wheels when the host Python
distribution cannot install scikit-learn. Mount the reviewed artifact read-only; do not
include CSV training data or a model artifact in the image.

The manifest starts with `approved: false`. Review per-class recall, high/critical false negatives,
Vietnamese/English slices, negation, quotations, slang, calibration and latency with a safety owner.
Approval is an operational step; do not infer it from the aggregate accuracy. Record the approval
outside this artifact, then set `approved: true` in the controlled deployment copy of the manifest.
Deploy that copy with restricted artifact access and a TLS endpoint. The backend uses
`MYLOG_SAFETY_CLASSIFIER_URL=https://.../classify` and a matching token from a secret manager.

Start the private service with access logging disabled so no request path or user text enters logs:

```powershell
$env:MYLOG_SAFETY_MODEL_DIR="artifacts/v1"
$env:MYLOG_SAFETY_CLASSIFIER_TOKEN="<secret-from-manager>"
.venv/Scripts/python -m uvicorn serve:app --host 127.0.0.1 --port 8091 --no-access-log
```

Use a trusted artifact only: `joblib` loading can execute code. Production approval also requires
an evaluated rule/classifier/policy combination in `safety_policy_versions`; enabling the service
alone never enables ordinary AI analysis. This baseline can later be replaced by a multilingual
encoder without changing the HTTP response contract.
