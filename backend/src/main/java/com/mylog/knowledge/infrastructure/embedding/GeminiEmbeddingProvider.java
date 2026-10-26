package com.mylog.knowledge.infrastructure.embedding;

import com.mylog.knowledge.application.EmbeddingProvider;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class GeminiEmbeddingProvider implements EmbeddingProvider {
    private final EmbeddingProperties properties;
    private final URI endpoint;
    private final ObjectMapper mapper;
    private final HttpClient client;

    GeminiEmbeddingProvider(EmbeddingProperties properties, ObjectMapper mapper) {
        this(properties, mapper, HttpClient.newHttpClient());
    }

    GeminiEmbeddingProvider(EmbeddingProperties properties, ObjectMapper mapper, HttpClient client) {
        this.properties = properties;
        this.mapper = mapper;
        this.client = client;
        if (!properties.model().matches("[A-Za-z0-9._-]{1,120}"))
            throw new IllegalArgumentException("Invalid embedding model");
        URI base = URI.create(properties.endpoint());
        boolean localHttp = "http".equals(base.getScheme())
                && ("127.0.0.1".equals(base.getHost()) || "localhost".equals(base.getHost()));
        if (base.getHost() == null || base.getUserInfo() != null || base.getQuery() != null
                || base.getFragment() != null || !("https".equals(base.getScheme()) || localHttp))
            throw new IllegalArgumentException("Invalid embedding endpoint");
        String root = base.toString().replaceAll("/+$", "");
        this.endpoint = URI.create(root + "/models/" + properties.model() + ":batchEmbedContents");
    }

    @Override
    public Batch embed(List<Input> inputs, TaskType taskType) {
        if (inputs == null || inputs.isEmpty() || inputs.size() > properties.batchSize())
            throw new IllegalArgumentException("Invalid embedding batch size");
        List<Map<String, Object>> requests = inputs.stream().map(input -> request(input, taskType)).toList();
        try {
            String body = mapper.writeValueAsString(Map.of("requests", requests));
            HttpRequest request = HttpRequest.newBuilder(endpoint)
                    .timeout(Duration.ofMillis(properties.timeoutMs()))
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", properties.apiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 400 || response.statusCode() == 401 || response.statusCode() == 403)
                throw new EmbeddingConfigurationException();
            if (response.statusCode() != 200 || response.body().length() > 10_000_000)
                throw new EmbeddingUnavailableException();
            return parse(response.body(), inputs.size());
        } catch (EmbeddingConfigurationException | EmbeddingUnavailableException
                 | InvalidEmbeddingOutputException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new EmbeddingUnavailableException();
        } catch (Exception exception) {
            throw new EmbeddingUnavailableException();
        }
    }

    private Map<String, Object> request(Input input, TaskType taskType) {
        if (input == null || input.text() == null || input.text().isBlank())
            throw new IllegalArgumentException("Embedding input is required");
        var request = new LinkedHashMap<String, Object>();
        request.put("model", "models/" + properties.model());
        request.put("content", Map.of("parts", List.of(Map.of("text", input.text()))));
        var config = new LinkedHashMap<String, Object>();
        config.put("taskType", taskType.name());
        config.put("outputDimensionality", properties.dimensions());
        if (taskType == TaskType.RETRIEVAL_DOCUMENT && input.title() != null && !input.title().isBlank())
            config.put("title", input.title());
        request.put("embedContentConfig", config);
        return request;
    }

    private Batch parse(String body, int expectedCount) throws Exception {
        JsonNode embeddings = mapper.readTree(body).get("embeddings");
        if (embeddings == null || !embeddings.isArray() || embeddings.size() != expectedCount)
            throw new InvalidEmbeddingOutputException();
        List<float[]> vectors = new ArrayList<>(expectedCount);
        for (JsonNode embedding : embeddings) {
            JsonNode values = embedding.get("values");
            if (values == null || !values.isArray() || values.size() != properties.dimensions())
                throw new InvalidEmbeddingOutputException();
            float[] vector = new float[properties.dimensions()];
            double squaredNorm = 0;
            for (int i = 0; i < vector.length; i++) {
                double value = values.get(i).doubleValue();
                if (!Double.isFinite(value)) throw new InvalidEmbeddingOutputException();
                vector[i] = (float) value;
                squaredNorm += value * value;
            }
            if (squaredNorm == 0) throw new InvalidEmbeddingOutputException();
            double norm = Math.sqrt(squaredNorm);
            for (int i = 0; i < vector.length; i++) vector[i] = (float) (vector[i] / norm);
            vectors.add(vector);
        }
        return new Batch("GEMINI", properties.model(), properties.modelVersion(),
                properties.dimensions(), List.copyOf(vectors));
    }

    static final class EmbeddingConfigurationException extends RuntimeException {}
    static final class EmbeddingUnavailableException extends RuntimeException {}
    static final class InvalidEmbeddingOutputException extends RuntimeException {}
}
