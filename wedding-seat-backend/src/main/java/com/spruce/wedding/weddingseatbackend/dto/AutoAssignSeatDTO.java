package com.spruce.wedding.weddingseatbackend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AutoAssignSeatDTO {

    @NotNull(message = "来宾ID不能为空")
    private Long guestId;

    @NotNull(message = "请填写需要分配的座位数")
    @Min(value = 1, message = "至少分配1个座位")
    @Max(value = 3, message = "每人最多可以选择3个座位")
    private Integer seatCount;
}
