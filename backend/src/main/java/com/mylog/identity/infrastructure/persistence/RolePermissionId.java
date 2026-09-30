package com.mylog.identity.infrastructure.persistence;

import java.io.Serializable;
import java.util.UUID;

record RolePermissionId(UUID roleId, UUID permissionId) implements Serializable {}
