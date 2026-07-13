package com.spruce.wedding.weddingseatbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 完整场地布局图：画布尺寸 + 场地元素（舞台/屏幕/出入口）+ 所有桌子（带坐标）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class VenueLayoutVO {
    private Integer canvasWidth;
    private Integer canvasHeight;
    private List<VenueElementVO> elements;
    private List<TableLayoutVO> tables;
}
