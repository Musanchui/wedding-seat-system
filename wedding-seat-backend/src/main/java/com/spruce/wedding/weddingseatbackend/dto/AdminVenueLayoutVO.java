package com.spruce.wedding.weddingseatbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 管理端"桌位大地图"整页数据：画布尺寸 + 场地元素 + 每桌详细座位(带来宾姓名手机号)
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminVenueLayoutVO {
    private Integer canvasWidth;
    private Integer canvasHeight;
    private List<VenueElementVO> elements;
    private List<AdminTableLayoutVO> tables;
}
