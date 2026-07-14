package com.spruce.wedding.weddingseatbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理端"看板视图"专用：能看到具体坐的是谁（姓名+手机号），
 * 来宾端的SeatVO没有这两个字段，注意别混用
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminSeatDetailVO {
    private Long id;
    private Integer seatNo;
    private Integer status;
    private String guestName;
    private String guestPhone;
}
