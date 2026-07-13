package com.spruce.wedding.weddingseatbackend.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * JWT工具类：管理员登录成功后签发token，后续请求靠这个token校验身份。
 *
 * 注意：SECRET_KEY 现在是硬编码在代码里的，这是为了让项目先跑起来、方便本地开发调试。
 * 部署到生产服务器之前，务必把这个密钥改成从环境变量读取（参考application.yml里其他敏感配置
 * 用 ${JWT_SECRET:默认值} 的写法），不要把生产环境用的密钥提交到Git仓库里。
 */
@Component
public class JwtUtil {

    /**
     * 签名密钥，长度必须满足HS256算法要求（至少256位，也就是至少32个字符）。
     * 这里随便生成了一个足够长的固定字符串，本地开发够用。
     */
    private static final String SECRET_STRING = "wedding-seat-system-jwt-secret-key-please-change-in-production-2026";

    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(SECRET_STRING.getBytes());

    /**
     * token有效期：7天（单位毫秒）。婚礼筹备周期可能持续几周，管理员不希望频繁重新登录，
     * 这个时长按实际使用场景来定，你也可以改短一点配合"记住我"之类的功能，现在先简单处理。
     */
    private static final long EXPIRATION_MILLIS = 7 * 24 * 60 * 60 * 1000L;

    /**
     * 生成token，把adminId编码进去（存在Claims的subject里）
     */
    public String generateToken(Long adminId) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + EXPIRATION_MILLIS);

        return Jwts.builder()
                .subject(String.valueOf(adminId))
                .issuedAt(now)
                .expiration(expiration)
                .signWith(SECRET_KEY)
                .compact();
    }

    /**
     * 解析token，成功则返回里面存的adminId；
     * 如果token过期、被篡改、格式不对，这里会直接抛出对应的运行时异常（ExpiredJwtException / JwtException等），
     * 调用方（JwtInterceptor）负责捕获并统一转换成"请重新登录"的提示。
     */
    public Long parseAdminId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(SECRET_KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return Long.valueOf(claims.getSubject());
    }

    /**
     * 判断token是否过期（不抛异常，安静地返回true/false，方便前端做"token快过期了提前续期"之类的判断，
     * 目前项目里没用到这个方法，先留着备用）
     */
    public boolean isExpired(String token) {
        try {
            parseAdminId(token);
            return false;
        } catch (ExpiredJwtException e) {
            return true;
        }
    }
}
