package com.spruce.wedding.weddingseatbackend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;

    @Value("${wedding.upload.base-path}")
    private String uploadBasePath;

    @Value("${wedding.upload.url-prefix}")
    private String uploadUrlPrefix;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                // context-path=/api已经全局生效，这里的路径不需要再带/api前缀
                .addPathPatterns("/admin/**")
                // 登录、注册这两个接口本身不需要token，排除掉，否则死循环
                .excludePathPatterns("/admin/login", "/admin/register");
    }

    /**
     * 把 wedding.upload.base-path 这个磁盘目录，映射成 wedding.upload.url-prefix 对应的URL路径。
     * 比如本地磁盘 D:/wedding-uploads/photo/1/xxx.jpg 这个文件，
     * 上传接口存进数据库的url是 /uploads/photo/1/xxx.jpg，
     * 来宾/浏览器访问 http://localhost:8080/api/uploads/photo/1/xxx.jpg 就能直接看到这张图。
     *
     * 注意：file:路径最后必须带斜杠，不然Spring会把它当成文件名前缀而不是目录，导致映射失败。
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = uploadBasePath.endsWith("/") || uploadBasePath.endsWith("\\")
                ? uploadBasePath
                : uploadBasePath + "/";
        registry.addResourceHandler(uploadUrlPrefix + "**")
                .addResourceLocations("file:" + location);
    }
}
