package com.spruce.wedding.weddingseatbackend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SeatLockDTO {

    @NotNull(message = "来宾ID不能为空")
    private Long guestId;

    @NotNull(message = "座位ID不能为空")
    private Long seatId;

    @NotNull(message = "version不能为空")
    private Integer version;
}
