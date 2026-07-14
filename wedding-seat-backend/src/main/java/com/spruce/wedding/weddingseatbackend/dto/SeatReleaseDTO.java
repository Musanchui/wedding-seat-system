package com.spruce.wedding.weddingseatbackend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SeatReleaseDTO {

    @NotNull(message = "来宾ID不能为空")
    private Long guestId;

    @NotNull(message = "座位ID不能为空")
    private Long seatId;
}
