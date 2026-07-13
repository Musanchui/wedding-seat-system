package com.spruce.wedding.weddingseatbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("wedding_event")
public class WeddingEvent {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long adminId;

    private String slug;

    private String groomName;

    private String brideName;

    private LocalDateTime eventTime;

    private String location;

    private String greetingMessage;

    private String musicUrl;

    /**
     * 场地布局画布尺寸，前端渲染场地图时的坐标系范围
     */
    private Integer layoutWidth;

    private Integer layoutHeight;

    /**
     * 0=筹备中（来宾端不可访问），1=已发布
     */
    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
