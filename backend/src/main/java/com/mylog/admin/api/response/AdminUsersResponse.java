package com.mylog.admin.api.response;

import java.util.List;
import java.util.UUID;

public record AdminUsersResponse(List<AdminUserResponse> items, UUID nextCursor) {}
