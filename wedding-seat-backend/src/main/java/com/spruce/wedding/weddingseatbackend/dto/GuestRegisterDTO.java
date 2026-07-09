package com.spruce.wedding.weddingseatbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class GuestRegisterDTO {

    @NotNull(message = "婚礼ID不能为空")
    private Long eventId;

    @NotBlank(message = "姓名不能为空")
    private String name;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /**
     * 来源类别，如"新郎同学"，可选字段，不传就走"空位最多的桌"兜底推荐逻辑
     */
    private String category;
}
