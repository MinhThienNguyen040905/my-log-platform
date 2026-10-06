package com.mylog.platform.web;

public final class DependencyUnavailableException extends ApplicationException {
    public DependencyUnavailableException() {
        super(ErrorCode.DEPENDENCY_UNAVAILABLE, "Tác vụ tạm thời chưa thể hoàn thành. Vui lòng thử lại sau.");
    }
}
