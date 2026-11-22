FROM python:3.12-slim
WORKDIR /app
RUN pip install --no-cache-dir torch --index-url https://download.pytorch.org/whl/cpu \
 && pip install --no-cache-dir "transformers>=4.40" safetensors underthesea
COPY predict.py .
# Weights không copy vào image; mount lúc chạy: -v ./emotion_model:/app/emotion_model:ro
ENTRYPOINT ["python", "predict.py"]
