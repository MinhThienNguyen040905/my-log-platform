FROM python:3.12-slim

WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY train.py serve.py ./

RUN useradd --system --uid 10001 model
USER model
EXPOSE 8091

# Mount only a reviewed artifact at MYLOG_SAFETY_MODEL_DIR and terminate TLS upstream.
CMD ["python", "-m", "uvicorn", "serve:app", "--host", "0.0.0.0", "--port", "8091", "--no-access-log"]
