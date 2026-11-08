package com.mylog.journal.application;

import com.mylog.platform.web.InvalidRequestException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class TipTapContent {
    private static final Set<String> CONTAINERS = Set.of("doc", "paragraph", "heading", "blockquote",
            "bulletList", "orderedList", "listItem");
    private static final Set<String> MARKS = Set.of("bold", "italic", "underline", "strike", "code");

    private TipTapContent() {}

    public static String plainText(JsonNode document) {
        if (document == null || !document.isObject() || !"doc".equals(text(document.get("type")))
                || document.toString().length() > 65_536) throw new InvalidRequestException();
        StringBuilder plain = new StringBuilder();
        int[] count = {0};
        visit(document, plain, 0, count);
        String value = plain.toString().trim();
        if (value.length() > 20_000) throw new InvalidRequestException();
        return value;
    }

    public static List<UUID> extractAssetIds(JsonNode document) {
        if (document == null || !document.isObject()) return List.of();
        List<UUID> list = new ArrayList<>();
        collectAssetIds(document, list, 0);
        return list;
    }

    public static JsonNode sanitizeForStorage(JsonNode document) {
        if (document == null || !document.isObject()) return document;
        JsonNode copy = document.deepCopy();
        sanitizeNode(copy, 0);
        return copy;
    }

    public static JsonNode enrichImageUrls(JsonNode document, Map<UUID, String> signedUrls) {
        if (document == null || !document.isObject()) return document;
        JsonNode copy = document.deepCopy();
        enrichNode(copy, signedUrls, 0);
        return copy;
    }

    private static void visit(JsonNode node, StringBuilder plain, int depth, int[] count) {
        if (!node.isObject() || depth > 20 || ++count[0] > 1_000) throw new InvalidRequestException();
        String type = text(node.get("type"));
        if ("text".equals(type)) {
            String value = text(node.get("text"));
            if (value == null || value.length() > 10_000 || node.size() > (node.get("marks") == null ? 2 : 3))
                throw new InvalidRequestException();
            JsonNode marks = node.get("marks");
            if (marks != null) {
                if (!marks.isArray()) throw new InvalidRequestException();
                for (JsonNode mark : marks) {
                    if (!mark.isObject() || mark.size() != 1 || !MARKS.contains(text(mark.get("type"))))
                        throw new InvalidRequestException();
                }
            }
            plain.append(value);
            return;
        }
        if ("hardBreak".equals(type)) {
            if (node.size() != 1) throw new InvalidRequestException();
            plain.append('\n');
            return;
        }
        if ("image".equals(type)) {
            JsonNode attrs = node.get("attrs");
            if (attrs == null || !attrs.isObject() || node.get("content") != null) throw new InvalidRequestException();
            String assetIdStr = text(attrs.get("data-asset-id"));
            if (assetIdStr == null || assetIdStr.length() != 36) throw new InvalidRequestException();
            try {
                UUID.fromString(assetIdStr);
            } catch (Exception e) {
                throw new InvalidRequestException();
            }
            JsonNode alt = attrs.get("alt");
            if (alt != null && (!alt.isTextual() || alt.asText().length() > 255)) throw new InvalidRequestException();
            JsonNode title = attrs.get("title");
            if (title != null && (!title.isTextual() || title.asText().length() > 255)) throw new InvalidRequestException();
            return;
        }
        if (!CONTAINERS.contains(type)) throw new InvalidRequestException();
        JsonNode attrs = node.get("attrs");
        int expected = node.get("content") == null ? 1 : 2;
        if (attrs != null) {
            if (!"heading".equals(type) || !attrs.isObject() || attrs.size() != 1
                    || attrs.get("level") == null || !attrs.get("level").isInt()
                    || attrs.get("level").asInt() < 1 || attrs.get("level").asInt() > 3)
                throw new InvalidRequestException();
            expected++;
        }
        if (node.size() != expected) throw new InvalidRequestException();
        JsonNode children = node.get("content");
        if (children != null) {
            if (!children.isArray()) throw new InvalidRequestException();
            for (JsonNode child : children) visit(child, plain, depth + 1, count);
        }
        if (Set.of("paragraph", "heading", "listItem", "blockquote").contains(type)) plain.append('\n');
    }

    private static void collectAssetIds(JsonNode node, List<UUID> list, int depth) {
        if (node == null || !node.isObject() || depth > 25) return;
        String type = text(node.get("type"));
        if ("image".equals(type)) {
            JsonNode attrs = node.get("attrs");
            if (attrs != null && attrs.isObject()) {
                String idStr = text(attrs.get("data-asset-id"));
                if (idStr != null) {
                    try {
                        list.add(UUID.fromString(idStr));
                    } catch (Exception ignored) {}
                }
            }
            return;
        }
        JsonNode children = node.get("content");
        if (children != null && children.isArray()) {
            for (JsonNode child : children) {
                collectAssetIds(child, list, depth + 1);
            }
        }
    }

    private static void sanitizeNode(JsonNode node, int depth) {
        if (node == null || !node.isObject() || depth > 25) return;
        String type = text(node.get("type"));
        if ("image".equals(type)) {
            JsonNode attrs = node.get("attrs");
            if (attrs instanceof ObjectNode obj) {
                obj.put("src", "");
            }
            return;
        }
        JsonNode children = node.get("content");
        if (children instanceof ArrayNode arr) {
            for (JsonNode child : arr) {
                sanitizeNode(child, depth + 1);
            }
        }
    }

    private static void enrichNode(JsonNode node, Map<UUID, String> signedUrls, int depth) {
        if (node == null || !node.isObject() || depth > 25) return;
        String type = text(node.get("type"));
        if ("image".equals(type)) {
            JsonNode attrs = node.get("attrs");
            if (attrs instanceof ObjectNode obj) {
                String assetIdStr = text(attrs.get("data-asset-id"));
                if (assetIdStr != null) {
                    try {
                        UUID assetId = UUID.fromString(assetIdStr);
                        String url = signedUrls.get(assetId);
                        if (url != null) {
                            obj.put("src", url);
                        }
                    } catch (Exception ignored) {}
                }
            }
            return;
        }
        JsonNode children = node.get("content");
        if (children instanceof ArrayNode arr) {
            for (JsonNode child : arr) {
                enrichNode(child, signedUrls, depth + 1);
            }
        }
    }

    private static String text(JsonNode node) {
        return node == null || !node.isTextual() ? null : node.asText();
    }
}
