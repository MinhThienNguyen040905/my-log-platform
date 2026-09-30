package com.mylog.platform.id;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

/** Generates RFC 9562 UUID version 7 identifiers. */
public final class UuidV7Generator implements IdGenerator {
    private static final long TIMESTAMP_MASK = 0x0000_FFFF_FFFF_FFFFL;
    private static final long RAND_A_MASK = 0x0FFFL;
    private static final long RAND_B_MASK = 0x3FFF_FFFF_FFFF_FFFFL;
    private static final long VERSION_7 = 0x7000L;
    private static final long RFC_4122_VARIANT = 0x8000_0000_0000_0000L;

    private final Clock clock;
    private final SecureRandom random;

    public UuidV7Generator(Clock clock, SecureRandom random) {
        this.clock = Objects.requireNonNull(clock);
        this.random = Objects.requireNonNull(random);
    }

    @Override
    public UUID next() {
        long timestamp = clock.millis() & TIMESTAMP_MASK;
        long randomA = random.nextLong() & RAND_A_MASK;
        long randomB = random.nextLong() & RAND_B_MASK;

        long mostSignificantBits = (timestamp << 16) | VERSION_7 | randomA;
        long leastSignificantBits = RFC_4122_VARIANT | randomB;
        return new UUID(mostSignificantBits, leastSignificantBits);
    }
}
