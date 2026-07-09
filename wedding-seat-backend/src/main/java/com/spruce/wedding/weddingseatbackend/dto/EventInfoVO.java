package com.spruce.wedding.weddingseatbackend.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 来宾端"欢迎页"用的婚礼基础信息展示对象，注意不包含 admin_id 等管理端内部字段
 */
@Data
public class EventInfoVO {
    private Long id;
    private String groomName;
    private String brideName;
    private LocalDateTime eventTime;
    private String location;
    private String greetingMessage;
    private String musicUrl;
}
