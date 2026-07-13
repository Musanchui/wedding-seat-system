package com.spruce.wedding.weddingseatbackend.config;

import com.spruce.wedding.weddingseatbackend.common.exception.BusinessException;
import com.spruce.wedding.weddingseatbackend.common.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private static final String TOKEN_HEADER = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String header = request.getHeader(TOKEN_HEADER);
        if (header == null || !header.startsWith(TOKEN_PREFIX)) {
            throw new BusinessException(401, "请先登录");
        }

        String token = header.substring(TOKEN_PREFIX.length());
        Long adminId;
        try {
            adminId = jwtUtil.parseAdminId(token);
        } catch (JwtException | IllegalArgumentException e) {
            // 涵盖token过期(ExpiredJwtException)、签名不对、格式损坏等所有JWT相关异常
            throw new BusinessException(401, "登录已过期，请重新登录");
        }

        // 把解析出来的adminId存进request属性，Controller方法里用 @RequestAttribute("adminId") Long adminId 就能直接拿到
        request.setAttribute("adminId", adminId);
        return true;
    }
}
