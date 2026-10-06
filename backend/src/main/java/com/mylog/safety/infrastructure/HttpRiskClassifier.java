package com.mylog.safety.infrastructure;

import com.mylog.safety.application.RiskClassifier;
import com.mylog.safety.application.RiskLevel;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/** Calls a separately deployed, authenticated inference service. No text or response is logged. */
final class HttpRiskClassifier implements RiskClassifier {
    private final URI endpoint;
    private final String token;
    private final Duration timeout;
    private final HttpClient client;
    private final ObjectMapper mapper;

    HttpRiskClassifier(String url, String token, int timeoutMs) {
        this(url, token, timeoutMs, HttpClient.newHttpClient(), new ObjectMapper());
    }

    HttpRiskClassifier(String url, String token, int timeoutMs, HttpClient client, ObjectMapper mapper) {
        URI uri = URI.create(url);
        boolean localHttp = "http".equalsIgnoreCase(uri.getScheme())
                && ("localhost".equalsIgnoreCase(uri.getHost()) || "127.0.0.1".equals(uri.getHost()));
        if (!("https".equalsIgnoreCase(uri.getScheme()) || localHttp) || uri.getHost() == null
                || uri.getUserInfo() != null || uri.getFragment() != null || uri.getQuery() != null
                || token == null || token.isBlank() || timeoutMs < 100 || timeoutMs > 10000) {
            throw new IllegalArgumentException("Invalid safety classifier configuration");
        }
        this.endpoint = uri;
        this.token = token;
        this.timeout = Duration.ofMillis(timeoutMs);
        this.client = client;
        this.mapper = mapper;
    }

    @Override public Optional<Classification> classify(String plainText) {
        try {
            String body = mapper.writeValueAsString(Map.of("text", plainText));
            HttpRequest request = HttpRequest.newBuilder(endpoint).timeout(timeout)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200 || response.body().length() > 8192) return Optional.empty();
            JsonNode json = mapper.readTree(response.body());
            JsonNode level = json.get("level");
            JsonNode confidence = json.get("confidence");
            JsonNode version = json.get("version");
            if (level == null || confidence == null || version == null || !confidence.isNumber())
                return Optional.empty();
            String modelVersion = version.asText();
            if (!modelVersion.matches("[A-Za-z0-9._:-]{1,80}")) return Optional.empty();
            BigDecimal score = confidence.decimalValue();
            if (score.signum() < 0 || score.compareTo(BigDecimal.ONE) > 0) return Optional.empty();
            return Optional.of(new Classification("MYLOG_INTERNAL", modelVersion,
                    RiskLevel.valueOf(level.asText()), score));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
