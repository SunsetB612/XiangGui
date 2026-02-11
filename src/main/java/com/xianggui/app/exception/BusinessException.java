package com.xianggui.app.exception;

import lombok.Getter;

/**
 * 业务异常
 * 用于封装业务逻辑错误（如余额不足、用户不存在等）
 * 
 * Why: 业务异常是预期内的错误，不需要告警，只需返回友好提示
 * Warning: 业务异常不应包含敏感技术信息
 */
@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;
    private final String errorType;

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.errorType = "BUSINESS_ERROR";
    }

    public BusinessException(Integer code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.errorType = "BUSINESS_ERROR";
    }

    public BusinessException(Integer code, String errorType, String message) {
        super(message);
        this.code = code;
        this.errorType = errorType;
    }
}
