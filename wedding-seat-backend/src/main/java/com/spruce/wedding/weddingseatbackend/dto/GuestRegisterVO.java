package com.spruce.wedding.weddingseatbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GuestRegisterVO {
    private Long guestId;

    /**
     * 推荐桌位：新来宾、或者老来宾但还没选任何座位时才会返回这个字段，否则为null
     */
    private RecommendTableVO recommendedTable;

    /**
     * 已选座位列表：老来宾已经选过至少1个座位时返回这个字段（可能1-3个），此时recommendedTable为null。
     * 前端可以据此判断：selectedSeats不为空就直接展示"您已选的座位"，为空才走"请选座"流程。
     */
    private List<SeatSummaryVO> selectedSeats;
}
