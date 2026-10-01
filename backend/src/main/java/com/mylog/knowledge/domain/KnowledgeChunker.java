package com.mylog.knowledge.domain;

import java.util.ArrayList;
import java.util.List;

public final class KnowledgeChunker {
    public static final String VERSION = "paragraph-800-v1";
    private KnowledgeChunker() {}

    public static List<String> chunks(String content) {
        var result = new ArrayList<String>();
        for (String paragraph : content.strip().split("\\R\\s*\\R")) {
            String text = paragraph.strip().replaceAll("\\s+", " ");
            while (text.length() > 800) {
                int boundary = text.lastIndexOf(' ', 800);
                if (boundary < 400) boundary = 800;
                result.add(text.substring(0, boundary).strip());
                text = text.substring(boundary).strip();
            }
            if (!text.isEmpty()) result.add(text);
        }
        if (result.isEmpty()) throw new IllegalArgumentException("empty content");
        return List.copyOf(result);
    }
}
