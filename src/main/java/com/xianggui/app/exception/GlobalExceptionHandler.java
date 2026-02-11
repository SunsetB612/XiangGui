package com.xianggui.app.exception;

import com.xianggui.app.common.ApiResponse;
import com.xianggui.app.common.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 统一处理系统中所有异常，区分业务异常与系统异常
 * 
 * Why: 集中异常处理逻辑，避免分散在各Controller中，确保错误响应格式统一
 * Warning: 系统异常需要记录完整堆栈，业务异常只需记录关键信息
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 处理业务异常
     * 业务异常是预期内的错误，返回友好提示，不记录堆栈
     */
    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusinessException(BusinessException e, HttpServletRequest request) {
        // 结构化日志：业务异常
        Map<String, Object> logData = new LinkedHashMap<>();
        logData.put("timestamp", LocalDateTime.now().format(DTF));
        logData.put("level", "WARN");
        logData.put("type", "BUSINESS_EXCEPTION");
        logData.put("errorCode", e.getCode());
        logData.put("errorType", e.getErrorType());
        logData.put("message", e.getMessage());
        logData.put("path", request.getRequestURI());
        logData.put("method", request.getMethod());

        log.warn("[业务异常] {}", logData);

        return ApiResponse.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理系统异常
     * 系统异常是非预期错误，需要记录完整堆栈以便排查
     */
    @ExceptionHandler(SystemException.class)
    public ApiResponse<Void> handleSystemException(SystemException e, HttpServletRequest request) {
        // 结构化日志：系统异常
        Map<String, Object> logData = new LinkedHashMap<>();
        logData.put("timestamp", LocalDateTime.now().format(DTF));
        logData.put("level", "ERROR");
        logData.put("type", "SYSTEM_EXCEPTION");
        logData.put("errorCode", e.getCode());
        logData.put("errorType", e.getErrorType());
        logData.put("message", e.getMessage());
        logData.put("path", request.getRequestURI());
        logData.put("method", request.getMethod());

        log.error("[系统异常] {}", logData, e);

        // 系统异常返回通用错误信息，避免暴露内部细节
        return ApiResponse.error(ErrorCode.INTERNAL_ERROR, "系统繁忙，请稍后重试");
    }

    /**
     * 处理参数校验异常（@Valid失败）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e, 
                                                                    HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        Map<String, Object> logData = new LinkedHashMap<>();
        logData.put("timestamp", LocalDateTime.now().format(DTF));
        logData.put("level", "WARN");
        logData.put("type", "PARAM_VALIDATION_ERROR");
        logData.put("message", message);
        logData.put("path", request.getRequestURI());
        logData.put("method", request.getMethod());

        log.warn("[参数校验失败] {}", logData);

        return ApiResponse.error(ErrorCode.PARAM_ERROR, "参数错误: " + message);
    }

    /**
     * 处理参数绑定异常
     */
    @ExceptionHandler(BindException.class)
    public ApiResponse<Void> handleBindException(BindException e, HttpServletRequest request) {
        String message = e.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        Map<String, Object> logData = new LinkedHashMap<>();
        logData.put("timestamp", LocalDateTime.now().format(DTF));
        logData.put("level", "WARN");
        logData.put("type", "PARAM_BIND_ERROR");
        logData.put("message", message);
        logData.put("path", request.getRequestURI());
        logData.put("method", request.getMethod());

        log.warn("[参数绑定失败] {}", logData);

        return ApiResponse.error(ErrorCode.PARAM_ERROR, "参数错误: " + message);
    }

    /**
     * 处理约束校验异常（@RequestParam校验失败）
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResponse<Void> handleConstraintViolationException(ConstraintViolationException e,
                                                                 HttpServletRequest request) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));

        Map<String, Object> logData = new LinkedHashMap<>();
        logData.put("timestamp", LocalDateTime.now().format(DTF));
        logData.put("level", "WARN");
        logData.put("type", "CONSTRAINT_VIOLATION");
        logData.put("message", message);
        logData.put("path", request.getRequestURI());
        logData.put("method", request.getMethod());

        log.warn("[约束校验失败] {}", logData);

        return ApiResponse.error(ErrorCode.PARAM_ERROR, "参数错误: " + message);
    }

    /**
     * 处理缺少必要参数异常
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ApiResponse<Void> handleMissingServletRequestParameterException(MissingServletRequestParameterException e,
                                                                            HttpServletRequest request) {
        Map<String, Object> logData = new LinkedHashMap<>();
        logData.put("timestamp", LocalDateTime.now().format(DTF));
        logData.put("level", "WARN");
        logData.put("type", "MISSING_PARAM");
        logData.put("paramName", e.getParameterName());
        logData.put("path", request.getRequestURI());
        logData.put("method", request.getMethod());

        log.warn("[缺少必要参数] {}", logData);

        return ApiResponse.error(ErrorCode.PARAM_ERROR, "缺少必要参数: " + e.getParameterName());
    }

    /**
     * 处理所有其他未捕获的异常
     * 作为最后一道防线，必须记录完整堆栈
     */
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception e, HttpServletRequest request) {
        Map<String, Object> logData = new LinkedHashMap<>();
        logData.put("timestamp", LocalDateTime.now().format(DTF));
        logData.put("level", "ERROR");
        logData.put("type", "UNKNOWN_EXCEPTION");
        logData.put("exceptionType", e.getClass().getSimpleName());
        logData.put("message", e.getMessage());
        logData.put("path", request.getRequestURI());
        logData.put("method", request.getMethod());

        log.error("[未捕获异常] {}", logData, e);

        return ApiResponse.error(ErrorCode.INTERNAL_ERROR, "系统繁忙，请稍后重试");
    }
}
