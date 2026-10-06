package com.mylog.support;

import com.mylog.platform.id.IdGenerator;

import java.util.UUID;

public final class FixedIdGenerator implements IdGenerator {
    private final UUID id;

    public FixedIdGenerator(UUID id) {
        this.id = id;
    }

    @Override
    public UUID next() {
        return id;
    }
}
