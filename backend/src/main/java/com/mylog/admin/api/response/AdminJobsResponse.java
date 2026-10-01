package com.mylog.admin.api.response;

import java.util.List;
import java.util.UUID;

public record AdminJobsResponse(List<AdminJobResponse> items, UUID nextCursor) {}
