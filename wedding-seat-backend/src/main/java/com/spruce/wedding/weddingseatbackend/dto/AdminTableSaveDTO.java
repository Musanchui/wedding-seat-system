package com.spruce.wedding.weddingseatbackend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 新增/编辑桌位共用一个DTO：id为null表示新增，id有值表示编辑
 */
@Data
public class AdminTableSaveDTO {

    private Long id;

    @NotNull(message = "婚礼ID不能为空")
    private Long eventId;

    @NotBlank(message = "桌号不能为空")
    private String tableNo;

    @NotNull(message = "座位数不能为空")
    @Min(value = 1, message = "座位数至少为1")
    private Integer seatCount;

    private String remark;

    private Integer posX;

    private Integer posY;

    private Integer rotation;
}
