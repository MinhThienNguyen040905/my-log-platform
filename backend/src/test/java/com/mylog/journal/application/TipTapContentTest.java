package com.mylog.journal.application;

import com.mylog.platform.web.InvalidRequestException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TipTapContentTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void extractsAssetIdsInOrder() throws Exception {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        String json = """
                {
                  "type": "doc",
                  "content": [
                    {
                      "type": "paragraph",
                      "content": [
                        { "type": "text", "text": "Hôm nay tôi đi chơi" }
                      ]
                    },
                    {
                      "type": "image",
                      "attrs": {
                        "data-asset-id": "%s",
                        "src": "https://example.com/ignored.jpg"
                      }
                    },
                    {
                      "type": "image",
                      "attrs": {
                        "data-asset-id": "%s"
                      }
                    }
                  ]
                }
                """.formatted(id1, id2);

        JsonNode doc = mapper.readTree(json);
        List<UUID> extracted = TipTapContent.extractAssetIds(doc);
        assertEquals(2, extracted.size());
        assertEquals(id1, extracted.get(0));
        assertEquals(id2, extracted.get(1));

        String plainText = TipTapContent.plainText(doc);
        assertEquals("Hôm nay tôi đi chơi", plainText);
    }

    @Test
    void rejectsImageNodeWithoutValidAssetId() throws Exception {
        String json = """
                {
                  "type": "doc",
                  "content": [
                    {
                      "type": "image",
                      "attrs": {
                        "src": "https://example.com/evil.jpg"
                      }
                    }
                  ]
                }
                """;

        JsonNode doc = mapper.readTree(json);
        assertThrows(InvalidRequestException.class, () -> TipTapContent.plainText(doc));
    }

    @Test
    void sanitizesImagesForStorageByClearingSrc() throws Exception {
        UUID id = UUID.randomUUID();
        String json = """
                {
                  "type": "doc",
                  "content": [
                    {
                      "type": "image",
                      "attrs": {
                        "data-asset-id": "%s",
                        "src": "https://temporary-signed-url.com/preview.jpg",
                        "alt": "Ảnh hoàng hôn"
                      }
                    }
                  ]
                }
                """.formatted(id);

        JsonNode doc = mapper.readTree(json);
        JsonNode sanitized = TipTapContent.sanitizeForStorage(doc);
        JsonNode imageNode = sanitized.get("content").get(0);
        assertEquals("", imageNode.get("attrs").get("src").asText());
        assertEquals(id.toString(), imageNode.get("attrs").get("data-asset-id").asText());
        assertEquals("Ảnh hoàng hôn", imageNode.get("attrs").get("alt").asText());
    }

    @Test
    void enrichesImageWithSignedUrl() throws Exception {
        UUID id = UUID.randomUUID();
        String json = """
                {
                  "type": "doc",
                  "content": [
                    {
                      "type": "image",
                      "attrs": {
                        "data-asset-id": "%s",
                        "src": ""
                      }
                    }
                  ]
                }
                """.formatted(id);

        JsonNode doc = mapper.readTree(json);
        String freshSignedUrl = "https://cloudinary.com/authenticated/s--abc--/v1/image.jpg";
        JsonNode enriched = TipTapContent.enrichImageUrls(doc, Map.of(id, freshSignedUrl));
        JsonNode imageNode = enriched.get("content").get(0);
        assertEquals(freshSignedUrl, imageNode.get("attrs").get("src").asText());
    }
}

