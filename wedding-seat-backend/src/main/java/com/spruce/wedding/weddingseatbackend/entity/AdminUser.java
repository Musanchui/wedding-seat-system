package com.spruce.wedding.weddingseatbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("admin_user")
public class AdminUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    /**
     * BCrypt加密后的密码，绝对不能直接返回给前端
     */
    private String password;

    private String phone;

    private String nickname;

    /**
     * 1=正常，0=禁用
     */
    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
