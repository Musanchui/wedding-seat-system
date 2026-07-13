package com.spruce.wedding.weddingseatbackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 注意：这里只引入了BCryptPasswordEncoder这一个类，并不是完整引入Spring Security框架，
 * 不会给项目加上Spring Security的自动安全拦截行为，纯粹只是借用它的密码加密算法实现。
 * BCryptPasswordEncoder这个类实际上打包在 spring-boot-starter-security 里，
 * 但因为我们只用这一个工具类，直接手写实现也可以；不过用Spring官方现成的更省心也更安全，
 * 所以pom.xml里需要加一个轻量依赖，具体见下方说明。
 */
@Configuration
public class PasswordConfig {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
