package com.spruce.wedding.weddingseatbackend.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EventUpdateDTO {

    private String slug;

    private String groomName;

    private String brideName;

    private LocalDateTime eventTime;

    private String location;

    private String greetingMessage;

    private String musicUrl;

    /**
     * 场地布局画布尺寸，配合"任务：场地布局"功能使用，不传就不修改原有值
     */
    private Integer layoutWidth;

    private Integer layoutHeight;

    /**
     * 0=筹备中（来宾端不可访问），1=已发布（来宾端可访问）。不传则不修改发布状态。
     */
    private Integer status;
}
