package com.spruce.wedding.weddingseatbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 来宾"我已选的座位"展示对象，一个来宾最多3条
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SeatSummaryVO {
    private Long seatId;
    private Long tableId;
    private String tableNo;
    private Integer seatNo;
}
