package com.spruce.wedding.weddingseatbackend.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.spruce.wedding.weddingseatbackend.common.exception.BusinessException;
import com.spruce.wedding.weddingseatbackend.dto.EventCreateDTO;
import com.spruce.wedding.weddingseatbackend.dto.EventDetailVO;
import com.spruce.wedding.weddingseatbackend.dto.EventListItemVO;
import com.spruce.wedding.weddingseatbackend.dto.EventUpdateDTO;
import com.spruce.wedding.weddingseatbackend.entity.WeddingEvent;
import com.spruce.wedding.weddingseatbackend.mapper.WeddingEventMapper;
import com.spruce.wedding.weddingseatbackend.service.AdminEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminEventServiceImpl implements AdminEventService {

    private final WeddingEventMapper weddingEventMapper;

    @Override
    public EventDetailVO create(Long adminId, EventCreateDTO dto) {
        String slug = dto.getSlug();
        if (slug == null || slug.isBlank()) {
            slug = generateRandomSlug();
        } else {
            checkSlugAvailable(slug, null);
        }

        WeddingEvent event = new WeddingEvent();
        event.setAdminId(adminId);
        event.setSlug(slug);
        event.setGroomName(dto.getGroomName());
        event.setBrideName(dto.getBrideName());
        event.setLayoutWidth(1000);
        event.setLayoutHeight(800);
        // 新建的婚礼默认是"筹备中"状态，来宾端访问不了，管理员自己编辑完信息、确认无误后再手动切换成"已发布"
        event.setStatus(0);
        weddingEventMapper.insert(event);

        return toDetailVO(event);
    }

    @Override
    public void update(Long adminId, Long eventId, EventUpdateDTO dto) {
        WeddingEvent event = getOwnedEvent(adminId, eventId);

        if (dto.getSlug() != null && !dto.getSlug().isBlank() && !dto.getSlug().equals(event.getSlug())) {
            checkSlugAvailable(dto.getSlug(), eventId);
            event.setSlug(dto.getSlug());
        }
        if (dto.getGroomName() != null) {
            event.setGroomName(dto.getGroomName());
        }
        if (dto.getBrideName() != null) {
            event.setBrideName(dto.getBrideName());
        }
        if (dto.getEventTime() != null) {
            event.setEventTime(dto.getEventTime());
        }
        if (dto.getLocation() != null) {
            event.setLocation(dto.getLocation());
        }
        if (dto.getGreetingMessage() != null) {
            event.setGreetingMessage(dto.getGreetingMessage());
        }
        if (dto.getMusicUrl() != null) {
            event.setMusicUrl(dto.getMusicUrl());
        }
        if (dto.getLayoutWidth() != null) {
            event.setLayoutWidth(dto.getLayoutWidth());
        }
        if (dto.getLayoutHeight() != null) {
            event.setLayoutHeight(dto.getLayoutHeight());
        }
        if (dto.getStatus() != null) {
            event.setStatus(dto.getStatus());
        }

        weddingEventMapper.updateById(event);
    }

    @Override
    public EventDetailVO getDetail(Long adminId, Long eventId) {
        WeddingEvent event = getOwnedEvent(adminId, eventId);
        return toDetailVO(event);
    }

    @Override
    public List<EventListItemVO> listMyEvents(Long adminId) {
        List<WeddingEvent> events = weddingEventMapper.selectList(
                Wrappers.<WeddingEvent>lambdaQuery()
                        .eq(WeddingEvent::getAdminId, adminId)
                        .orderByDesc(WeddingEvent::getCreatedAt)
        );
        return events.stream()
                .map(e -> new EventListItemVO(e.getId(), e.getSlug(), e.getGroomName(),
                        e.getBrideName(), e.getEventTime(), e.getStatus()))
                .toList();
    }

    /**
     * 越权校验核心方法：查询这场婚礼，并且确认它属于当前登录的管理员。
     * 所有"针对某一场具体婚礼"的操作都必须经过这一步，这是多租户系统的安全底线，不能省略。
     */
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

    private void checkSlugAvailable(String slug, Long excludeEventId) {
        WeddingEvent existing = weddingEventMapper.selectOne(
                Wrappers.<WeddingEvent>lambdaQuery().eq(WeddingEvent::getSlug, slug)
        );
        if (existing != null && !existing.getId().equals(excludeEventId)) {
            throw new BusinessException("这个访问标识已经被使用了，换一个试试");
        }
    }

    private String generateRandomSlug() {
        // 简单生成一个8位随机字符串作为默认slug，管理员后续可以通过编辑接口改成更好记的名字（比如新人姓名拼音+日期）
        String candidate = "wedding-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        // 极小概率重复，重复的话再生成一次（实际发生概率可以忽略不计，这里只是严谨起见）
        while (weddingEventMapper.selectCount(
                Wrappers.<WeddingEvent>lambdaQuery().eq(WeddingEvent::getSlug, candidate)) > 0) {
            candidate = "wedding-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        }
        return candidate;
    }

    private EventDetailVO toDetailVO(WeddingEvent event) {
        return new EventDetailVO(
                event.getId(), event.getSlug(), event.getGroomName(), event.getBrideName(),
                event.getEventTime(), event.getLocation(), event.getGreetingMessage(),
                event.getMusicUrl(), event.getLayoutWidth(), event.getLayoutHeight(), event.getStatus()
        );
    }
}
