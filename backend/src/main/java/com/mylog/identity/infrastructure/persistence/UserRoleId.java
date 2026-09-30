package com.mylog.identity.infrastructure.persistence;

import java.io.Serializable;
import java.util.UUID;

record UserRoleId(UUID userId, UUID roleId) implements Serializable {}
