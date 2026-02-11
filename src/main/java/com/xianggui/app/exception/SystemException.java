package com.xianggui.app.exception;

import lombok.Getter;

/**
 * 系统异常
 * 用于封装系统级错误（如DB连接失败、Redis不可用等）
 * 
 * Why: 区分业务异常与系统异常，便于差异化处理和监控
 * Warning: 系统异常通常需要立即通知运维人员
 */
@Getter
public class SystemException extends RuntimeException {

    private final Integer code;
    private final String errorType;

    public SystemException(Integer code, String message) {
        super(message);
        this.code = code;
        this.errorType = "SYSTEM_ERROR";
    }

    public SystemException(Integer code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.errorType = "SYSTEM_ERROR";
    }

    public SystemException(Integer code, String errorType, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.errorType = errorType;
    }
}
