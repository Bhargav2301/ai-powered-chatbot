FROM python:3.12-slim
WORKDIR /app
COPY services/rag/requirements.txt ./requirements.txt
RUN pip install --no-cache-dir -r requirements.txt
COPY services/rag/polymath_rag ./polymath_rag
RUN useradd --uid 10001 --create-home polymath
USER 10001
ENV POLYMATH_MODEL_DIR=/models
EXPOSE 8000
CMD ["uvicorn", "polymath_rag.app:create_app", "--factory", "--host", "0.0.0.0", "--port", "8000", "--no-access-log"]
