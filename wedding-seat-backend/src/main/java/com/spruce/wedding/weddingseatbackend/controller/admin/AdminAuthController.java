package com.spruce.wedding.weddingseatbackend.controller.admin;

import com.spruce.wedding.weddingseatbackend.common.result.Result;
import com.spruce.wedding.weddingseatbackend.dto.AdminLoginDTO;
import com.spruce.wedding.weddingseatbackend.dto.AdminLoginVO;
import com.spruce.wedding.weddingseatbackend.dto.AdminRegisterDTO;
import com.spruce.wedding.weddingseatbackend.service.AdminAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 注意：这两个接口(注册、登录)必须在 JwtInterceptor 的拦截排除名单里，
 * 不然会陷入"登录前先要有token，但token要靠登录才能拿到"的死循环。
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody AdminRegisterDTO dto) {
        adminAuthService.register(dto);
        return Result.success();
    }

    @PostMapping("/login")
    public Result<AdminLoginVO> login(@Valid @RequestBody AdminLoginDTO dto) {
        return Result.success(adminAuthService.login(dto));
    }
}
