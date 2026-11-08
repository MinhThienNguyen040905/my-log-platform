package com.mylog.platform.text;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VietnameseStringUtilsTest {

    @Test
    void normalizesVietnameseDiacritics() {
        assertEquals("da lat", VietnameseStringUtils.normalize("Đà Lạt"));
        assertEquals("chuyen di da lat mong mo", VietnameseStringUtils.normalize("Chuyến đi Đà Lạt mộng mơ"));
        assertEquals("hom nay toi rat vui!", VietnameseStringUtils.normalize("Hôm nay tôi rất vui!"));
        assertEquals("an com", VietnameseStringUtils.normalize("Ăn cơm"));
        assertEquals("viet nam", VietnameseStringUtils.normalize("Việt Nam"));
        assertEquals("", VietnameseStringUtils.normalize(null));
        assertEquals("", VietnameseStringUtils.normalize("   "));
    }

    @Test
    void preservesAlphanumericAndRemovesCombiningMarks() {
        assertEquals("hello world", VietnameseStringUtils.normalize("Hello World"));
        assertEquals("123 test", VietnameseStringUtils.normalize("123 TEST"));
    }
}
