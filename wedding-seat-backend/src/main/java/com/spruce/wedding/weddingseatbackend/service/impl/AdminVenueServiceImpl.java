package com.spruce.wedding.weddingseatbackend.service.impl;

import com.spruce.wedding.weddingseatbackend.common.exception.BusinessException;
import com.spruce.wedding.weddingseatbackend.dto.AdminVenueElementSaveDTO;
import com.spruce.wedding.weddingseatbackend.entity.VenueElement;
import com.spruce.wedding.weddingseatbackend.entity.WeddingEvent;
import com.spruce.wedding.weddingseatbackend.mapper.VenueElementMapper;
import com.spruce.wedding.weddingseatbackend.mapper.WeddingEventMapper;
import com.spruce.wedding.weddingseatbackend.service.AdminVenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminVenueServiceImpl implements AdminVenueService {

    private final WeddingEventMapper weddingEventMapper;
    private final VenueElementMapper venueElementMapper;

    @Override
    public void saveElement(Long adminId, AdminVenueElementSaveDTO dto) {
        getOwnedEvent(adminId, dto.getEventId());

        VenueElement element;
        if (dto.getId() == null) {
            element = new VenueElement();
            element.setEventId(dto.getEventId());
            element.setSortOrder(0);
        } else {
            element = venueElementMapper.selectById(dto.getId());
            if (element == null) {
                throw new BusinessException("场地元素不存在");
            }
            // 双重校验：这个元素所属的event也必须归属当前管理员
            getOwnedEvent(adminId, element.getEventId());
        }

        element.setType(dto.getType());
        element.setLabel(dto.getLabel());
        element.setPosX(dto.getPosX());
        element.setPosY(dto.getPosY());
        if (dto.getWidth() != null) {
            element.setWidth(dto.getWidth());
        }
        if (dto.getHeight() != null) {
            element.setHeight(dto.getHeight());
        }
        if (dto.getRotation() != null) {
            element.setRotation(dto.getRotation());
        }

        if (dto.getId() == null) {
            venueElementMapper.insert(element);
        } else {
            venueElementMapper.updateById(element);
        }
    }

    @Override
    public void deleteElement(Long adminId, Long elementId) {
        VenueElement element = venueElementMapper.selectById(elementId);
        if (element == null) {
            throw new BusinessException("场地元素不存在");
        }
        getOwnedEvent(adminId, element.getEventId());
        venueElementMapper.deleteById(elementId);
    }

    private WeddingEvent getOwnedEvent(Long adminId, Long eventId) {
        WeddingEvent event = weddingEventMapper.selectById(eventId);
        if (event == null) {
            throw new BusinessException("婚礼不存在");
        }
        if (!event.getAdminId().equals(adminId)) {
            throw new BusinessException(403, "无权操作该婚礼");
        }
        return event;
    }
}
