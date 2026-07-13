package com.spruce.wedding.weddingseatbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 来宾端"查看全部桌位布局"用的展示对象
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TableSummaryVO {
    private Long id;
    private String tableNo;
    private String remark;
    private Integer seatCount;
    private Integer availableSeatsCount;
}
