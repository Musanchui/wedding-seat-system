package com.spruce.wedding.weddingseatbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminTableLayoutVO {
    private Long id;
    private String tableNo;
    private Integer seatCount;
    private String remark;
    private Integer posX;
    private Integer posY;
    private Integer rotation;
    private List<AdminSeatDetailVO> seats;
}
