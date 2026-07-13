package com.spruce.wedding.weddingseatbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 场地图里单张桌子的展示对象：带坐标+旋转角度+可用座位数
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TableLayoutVO {
    private Long id;
    private String tableNo;
    private String remark;
    private Integer seatCount;
    private Integer availableSeatsCount;
    private Integer posX;
    private Integer posY;
    private Integer rotation;
}
