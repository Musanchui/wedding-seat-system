package com.spruce.wedding.weddingseatbackend.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.spruce.wedding.weddingseatbackend.common.exception.BusinessException;
import com.spruce.wedding.weddingseatbackend.common.util.JwtUtil;
import com.spruce.wedding.weddingseatbackend.dto.AdminLoginDTO;
import com.spruce.wedding.weddingseatbackend.dto.AdminLoginVO;
import com.spruce.wedding.weddingseatbackend.dto.AdminRegisterDTO;
import com.spruce.wedding.weddingseatbackend.entity.AdminUser;
import com.spruce.wedding.weddingseatbackend.mapper.AdminUserMapper;
import com.spruce.wedding.weddingseatbackend.service.AdminAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminAuthServiceImpl implements AdminAuthService {

    private final AdminUserMapper adminUserMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public void register(AdminRegisterDTO dto) {
        Long count = adminUserMapper.selectCount(
                Wrappers.<AdminUser>lambdaQuery().eq(AdminUser::getUsername, dto.getUsername())
        );
        if (count > 0) {
            throw new BusinessException("该用户名已被注册，换一个试试");
        }

        AdminUser admin = new AdminUser();
        admin.setUsername(dto.getUsername());
        // 密码加密后存储，绝对不能存明文
        admin.setPassword(passwordEncoder.encode(dto.getPassword()));
        admin.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());
        admin.setPhone(dto.getPhone());
        admin.setStatus(1);
        adminUserMapper.insert(admin);
    }

    @Override
    public AdminLoginVO login(AdminLoginDTO dto) {
        AdminUser admin = adminUserMapper.selectOne(
                Wrappers.<AdminUser>lambdaQuery().eq(AdminUser::getUsername, dto.getUsername())
        );
        if (admin == null) {
            // 故意用同一句"用户名或密码错误"，不单独提示"用户名不存在"，防止别人借此枚举出哪些用户名已注册
            throw new BusinessException("用户名或密码错误");
        }
        if (admin.getStatus() == null || admin.getStatus() != 1) {
            throw new BusinessException("该账号已被禁用，请联系管理员");
        }
        if (!passwordEncoder.matches(dto.getPassword(), admin.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        String token = jwtUtil.generateToken(admin.getId());
        return new AdminLoginVO(admin.getId(), admin.getUsername(), admin.getNickname(), token);
    }
}
