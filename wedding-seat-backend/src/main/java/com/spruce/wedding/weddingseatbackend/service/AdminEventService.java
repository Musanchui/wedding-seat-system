package com.spruce.wedding.weddingseatbackend.service;

import com.spruce.wedding.weddingseatbackend.dto.EventCreateDTO;
import com.spruce.wedding.weddingseatbackend.dto.EventDetailVO;
import com.spruce.wedding.weddingseatbackend.dto.EventListItemVO;
import com.spruce.wedding.weddingseatbackend.dto.EventUpdateDTO;

import java.util.List;

public interface AdminEventService {

    EventDetailVO create(Long adminId, EventCreateDTO dto);

    void update(Long adminId, Long eventId, EventUpdateDTO dto);

    EventDetailVO getDetail(Long adminId, Long eventId);

    List<EventListItemVO> listMyEvents(Long adminId);
}
