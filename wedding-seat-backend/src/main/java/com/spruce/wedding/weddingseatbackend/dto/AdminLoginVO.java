package com.spruce.wedding.weddingseatbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminLoginVO {
    private Long adminId;
    private String username;
    private String nickname;
    private String token;
}
