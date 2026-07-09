package com.spruce.wedding.weddingseatbackend.service;

import com.spruce.wedding.weddingseatbackend.dto.EventInfoVO;
import com.spruce.wedding.weddingseatbackend.dto.GuestRegisterDTO;
import com.spruce.wedding.weddingseatbackend.dto.GuestRegisterVO;
import com.spruce.wedding.weddingseatbackend.dto.PhotoVO;
import com.spruce.wedding.weddingseatbackend.dto.SeatVO;

import java.util.List;

public interface GuestService {

    EventInfoVO getEventInfo(Long eventId);

    List<PhotoVO> getEventPhotos(Long eventId);

    GuestRegisterVO register(GuestRegisterDTO dto);

    List<SeatVO> getTableSeats(Long tableId);

    void lockSeat(Long guestId, Long seatId, Integer version);
}
