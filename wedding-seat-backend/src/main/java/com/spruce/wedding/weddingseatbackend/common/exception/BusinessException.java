package com.spruce.wedding.weddingseatbackend.common.exception;

import lombok.Getter;

/**
 * 通用业务异常，携带一个code，方便区分普通业务错误(400)和座位冲突(409)等场景
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = 400;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 座位被抢占专用异常，固定code=409，Service层选座失败时直接抛这个
     */
    public static class SeatConflictException extends BusinessException {
        public SeatConflictException(String message) {
            super(409, message);
        }
    }
}
