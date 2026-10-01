package com.mylog.platform.id;

import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class UuidV7GeneratorTest {
    private static final Instant FIXED_INSTANT = Instant.parse("2026-09-30T03:00:00Z");

    @Test
    void generatesVersionSevenUuidWithRfcVariantAndEmbeddedTimestamp() {
        var generator = new UuidV7Generator(
                Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC),
                new SecureRandom()
        );

        var id = generator.next();
        long extractedTimestamp = (id.getMostSignificantBits() >>> 16) & 0x0000_FFFF_FFFF_FFFFL;

        assertThat(id.version()).isEqualTo(7);
        assertThat(id.variant()).isEqualTo(2);
        assertThat(extractedTimestamp).isEqualTo(FIXED_INSTANT.toEpochMilli());
    }

    @Test
    void producesDifferentIdentifiersAtTheSameMillisecond() {
        var generator = new UuidV7Generator(
                Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC),
                new SecureRandom()
        );

        assertThat(generator.next()).isNotEqualTo(generator.next());
    }
}
