package com.mylog.platform.web;

public final class ResourceNotFoundException extends ApplicationException {
    public ResourceNotFoundException(String safeDetail) {
        super(ErrorCode.RESOURCE_NOT_FOUND, safeDetail);
    }
}
