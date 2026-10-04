package com.mylog.safety.infrastructure;

import com.mylog.safety.application.RiskLevel;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class HttpRiskClassifierTest {
    @Test void sendsOnlyTextWithServerTokenAndParsesVersionedClassification() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<String> body = new AtomicReference<>();
        AtomicReference<String> authorization = new AtomicReference<>();
        server.createContext("/classify", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] result = "{\"level\":\"LOW\",\"confidence\":0.91,\"version\":\"v1\"}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, result.length);
            exchange.getResponseBody().write(result);
            exchange.close();
        });
        server.start();
        try {
            var classifier = new HttpRiskClassifier(
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/classify", "test-secret", 2000);
            var result = classifier.classify("Synthetic calm text").orElseThrow();
            assertEquals(RiskLevel.LOW, result.level());
            assertEquals("v1", result.version());
            assertEquals("MYLOG_INTERNAL", result.provider());
            assertEquals("Bearer test-secret", authorization.get());
            assertEquals("{\"text\":\"Synthetic calm text\"}", body.get());
        } finally {
            server.stop(0);
        }
    }

    @Test void rejectsNonTlsRemoteEndpoint() {
        assertThrows(IllegalArgumentException.class,
                () -> new HttpRiskClassifier("http://example.com/classify", "token", 2000));
    }
}
