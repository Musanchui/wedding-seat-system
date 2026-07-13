package com.spruce.wedding.weddingseatbackend.service;

import com.spruce.wedding.weddingseatbackend.dto.AdminLoginDTO;
import com.spruce.wedding.weddingseatbackend.dto.AdminLoginVO;
import com.spruce.wedding.weddingseatbackend.dto.AdminRegisterDTO;

public interface AdminAuthService {

    void register(AdminRegisterDTO dto);

    AdminLoginVO login(AdminLoginDTO dto);
}
