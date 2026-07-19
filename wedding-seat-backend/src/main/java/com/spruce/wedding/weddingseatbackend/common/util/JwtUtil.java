package com.spruce.wedding.weddingseatbackend.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * JWT工具类：管理员登录成功后签发token，后续请求靠这个token校验身份。
 *
 * 密钥从 application.yml 的 jwt.secret 读取（支持环境变量 JWT_SECRET 覆盖），
 * 本地开发用yml里的默认值即可；部署到生产服务器时，必须通过环境变量传入一个
 * 只有你自己知道的随机字符串，不能用默认值，否则任何拿到源码的人都能伪造登录token。
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secretString;

    private SecretKey secretKey;

    /**
     * token有效期：7天（单位毫秒）。婚礼筹备周期可能持续几周，管理员不希望频繁重新登录，
     * 这个时长按实际使用场景来定，你也可以改短一点配合"记住我"之类的功能，现在先简单处理。
     */
    private static final long EXPIRATION_MILLIS = 7 * 24 * 60 * 60 * 1000L;

    /**
     * Spring注入完@Value之后自动执行一次，把字符串密钥转换成HS256算法需要的SecretKey对象。
     * 顺便校验长度：HS256要求密钥至少256位(32个字符)，长度不够直接在启动时报错，
     * 而不是等到真正登录时才出问题，暴露问题的时机越早越好。
     */
    @PostConstruct
    public void init() {
        if (secretString == null || secretString.getBytes().length < 32) {
            throw new IllegalStateException(
                    "JWT密钥长度不够(至少需要32个字符/256位)，请检查环境变量 JWT_SECRET 是否已正确设置");
        }
        this.secretKey = Keys.hmacShaKeyFor(secretString.getBytes());
    }

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
                .signWith(secretKey)
                .compact();
    }

    /**
     * 解析token，成功则返回里面存的adminId；
     * 如果token过期、被篡改、格式不对，这里会直接抛出对应的运行时异常（ExpiredJwtException / JwtException等），
     * 调用方（JwtInterceptor）负责捕获并统一转换成"请重新登录"的提示。
     */
    public Long parseAdminId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
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
