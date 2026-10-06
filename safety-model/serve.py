"""Private inference endpoint. Put behind TLS; never expose it to the public internet."""

import hashlib
import hmac
import json
import os
from pathlib import Path

import joblib
from fastapi import FastAPI, Header, HTTPException, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from pydantic import BaseModel, Field

MODEL_DIR = Path(os.environ["MYLOG_SAFETY_MODEL_DIR"])
TOKEN = os.environ["MYLOG_SAFETY_CLASSIFIER_TOKEN"]
if not TOKEN:
    raise RuntimeError("Missing classifier token")
MANIFEST = json.loads((MODEL_DIR / "manifest.json").read_text(encoding="utf-8"))
ARTIFACT = MODEL_DIR / "model.joblib"
if hashlib.sha256(ARTIFACT.read_bytes()).hexdigest() != MANIFEST["artifact_sha256"]:
    raise RuntimeError("Model artifact checksum mismatch")
if MANIFEST.get("approved") is not True:
    raise RuntimeError("Model has not been approved for inference")
# joblib artifacts are pickle-based: load only an artifact produced by the trusted pipeline.
MODEL = joblib.load(ARTIFACT)
MIN_CONFIDENCE = float(os.environ.get("MYLOG_SAFETY_MODEL_MIN_CONFIDENCE", "0.8"))
if not 0 <= MIN_CONFIDENCE <= 1:
    raise RuntimeError("Invalid confidence threshold")

app = FastAPI(docs_url=None, redoc_url=None, openapi_url=None)


@app.exception_handler(RequestValidationError)
async def validation_error(_request: Request, _error: RequestValidationError):
    return JSONResponse(status_code=422, content={"detail": "Invalid classification request"})


class ClassificationRequest(BaseModel):
    text: str = Field(min_length=1, max_length=20_000)


class ClassificationResponse(BaseModel):
    level: str
    confidence: float
    version: str


@app.post("/classify", response_model=ClassificationResponse)
def classify(request: ClassificationRequest, authorization: str | None = Header(default=None)):
    expected = "Bearer " + TOKEN
    if authorization is None or not hmac.compare_digest(authorization, expected):
        raise HTTPException(status_code=401, detail="Unauthorized")
    scores = MODEL.predict_proba([request.text])[0]
    index = int(scores.argmax())
    confidence = float(scores[index])
    level = str(MODEL.classes_[index]) if confidence >= MIN_CONFIDENCE else "UNKNOWN"
    return ClassificationResponse(level=level, confidence=confidence,
                                  version=MANIFEST["version"])
