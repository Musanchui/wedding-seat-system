package com.spruce.wedding.weddingseatbackend.common.exception;

import com.spruce.wedding.weddingseatbackend.common.result.Result;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常（包含409座位冲突这种子类），直接把异常里的code和message透传给前端
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * DTO参数校验失败（@Valid触发），取第一个字段错误信息返回，前端能直接展示给用户
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidException(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "参数校验失败";
        return Result.fail(message);
    }

    /**
     * 兜底：其他未预料到的异常，不要把堆栈信息暴露给前端，只给通用提示，详细堆栈打到日志里
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        e.printStackTrace(); // 建议后续替换成正式的日志框架输出，比如 log.error("系统异常", e)
        return Result.fail(500, "服务器开小差了，请稍后再试");
    }
}
