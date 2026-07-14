package com.spruce.wedding.weddingseatbackend.service;

import com.spruce.wedding.weddingseatbackend.dto.EventInfoVO;
import com.spruce.wedding.weddingseatbackend.dto.GuestRegisterDTO;
import com.spruce.wedding.weddingseatbackend.dto.GuestRegisterVO;
import com.spruce.wedding.weddingseatbackend.dto.PhotoVO;
import com.spruce.wedding.weddingseatbackend.dto.SeatSummaryVO;
import com.spruce.wedding.weddingseatbackend.dto.SeatVO;
import com.spruce.wedding.weddingseatbackend.dto.TableSummaryVO;
import com.spruce.wedding.weddingseatbackend.dto.VenueLayoutVO;

import java.util.List;

public interface GuestService {

    EventInfoVO getEventInfo(String slug);

    List<PhotoVO> getEventPhotos(String slug);

    /**
     * 获取该婚礼所有启用的桌位及各桌可用座位数，供来宾自主浏览挑选（不局限于系统推荐的那一桌）
     */
    List<TableSummaryVO> getEventTables(String slug);

    /**
     * 获取完整场地布局：画布尺寸 + 舞台/屏幕/出入口等元素 + 所有桌子(带坐标)
     */
    VenueLayoutVO getVenueLayout(String slug);

    GuestRegisterVO register(GuestRegisterDTO dto);

    List<SeatVO> getTableSeats(Long tableId);

    /**
     * 锁定座位。一个来宾最多可以锁定3个座位，超过会拒绝。
     */
    void lockSeat(Long guestId, Long seatId, Integer version);

    /**
     * 释放(取消)已选的某一个座位，来宾用于"选错了/想换一个"的场景
     */
    void releaseSeat(Long guestId, Long seatId);

    /**
     * 查询某个来宾当前已经选定的所有座位（0-3个）
     */
    List<SeatSummaryVO> getMySeats(Long guestId);
}
