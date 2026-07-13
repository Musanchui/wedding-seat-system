package com.spruce.wedding.weddingseatbackend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                // context-path=/api已经全局生效，这里的路径不需要再带/api前缀
                .addPathPatterns("/admin/**")
                // 登录、注册这两个接口本身不需要token，排除掉，否则死循环
                .excludePathPatterns("/admin/login", "/admin/register");
    }

    // 注意：素材上传功能做好之后，这里还需要补一个 addResourceHandlers() 方法，
    // 把 wedding.upload.base-path 映射成 wedding.upload.url-prefix 对应的静态资源访问路径，
    // 现在先不加，等做上传功能那一步再回来补。
}
