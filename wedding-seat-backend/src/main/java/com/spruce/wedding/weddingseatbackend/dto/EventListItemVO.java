package com.spruce.wedding.weddingseatbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 管理端"我的婚礼列表"用的精简展示对象
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventListItemVO {
    private Long id;
    private String slug;
    private String groomName;
    private String brideName;
    private LocalDateTime eventTime;
    private Integer status;
}
