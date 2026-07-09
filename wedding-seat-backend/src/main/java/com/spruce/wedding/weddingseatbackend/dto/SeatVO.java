package com.spruce.wedding.weddingseatbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 座位状态展示对象。注意：不返回guestName/guestPhone给来宾端，
 * 已占用的座位只标记status=1即可，避免暴露其他来宾隐私。
 * (管理端的"看板视图"是单独一个带guestName/guestPhone的VO，属于admin模块任务，这里不重复)
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SeatVO {
    private Long id;
    private Integer seatNo;
    private Integer status;
    private Integer version;
}
