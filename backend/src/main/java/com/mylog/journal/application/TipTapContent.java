package com.mylog.journal.application;

import com.mylog.platform.web.InvalidRequestException;
import tools.jackson.databind.JsonNode;

import java.util.Set;

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

    private static String text(JsonNode node) {
        return node == null || !node.isTextual() ? null : node.asText();
    }
}
