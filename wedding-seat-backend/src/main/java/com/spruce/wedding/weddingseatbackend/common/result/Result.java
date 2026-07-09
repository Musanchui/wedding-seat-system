package com.spruce.wedding.weddingseatbackend.common.result;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用返回结果封装
 * code: 200=成功；409=座位并发冲突（专门给抢座场景用，方便前端针对性处理触发局部刷新重选）；
 *       400=参数/业务错误；401=未登录或token失效；500=服务器异常
 */
@Data
public class Result<T> implements Serializable {

    private int code;
    private String message;
    private T data;

    public Result() {
    }

    public Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }

    public static <T> Result<T> success() {
        return new Result<>(200, "success", null);
    }

    public static <T> Result<T> fail(String message) {
        return new Result<>(400, message, null);
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }

    /**
     * 座位并发冲突专用：前端约定收到code=409时，触发局部刷新重新选座
     */
    public static <T> Result<T> conflict(String message) {
        return new Result<>(409, message, null);
    }
}
