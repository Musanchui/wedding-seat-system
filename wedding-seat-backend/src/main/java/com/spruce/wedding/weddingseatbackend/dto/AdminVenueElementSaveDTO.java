package com.spruce.wedding.weddingseatbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminVenueElementSaveDTO {

    private Long id;

    @NotNull(message = "婚礼ID不能为空")
    private Long eventId;

    @NotBlank(message = "元素类型不能为空")
    private String type;

    private String label;

    @NotNull(message = "X坐标不能为空")
    private Integer posX;

    @NotNull(message = "Y坐标不能为空")
    private Integer posY;

    private Integer width;

    private Integer height;

    private Integer rotation;
}
