package com.spruce.wedding.weddingseatbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 管理端查看婚礼详情用，包含slug、status这些管理端才关心的字段
 * (来宾端的EventInfoVO不包含这些，两个VO故意分开，避免混用)
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventDetailVO {
    private Long id;
    private String slug;
    private String groomName;
    private String brideName;
    private LocalDateTime eventTime;
    private String location;
    private String greetingMessage;
    private String musicUrl;
    private Integer layoutWidth;
    private Integer layoutHeight;
    private Integer status;
}
