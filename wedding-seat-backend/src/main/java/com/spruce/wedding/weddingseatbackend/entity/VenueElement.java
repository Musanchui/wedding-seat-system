package com.spruce.wedding.weddingseatbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 场地元素：舞台、屏幕、出入口等非桌子的场地标注
 */
@Data
@TableName("venue_element")
public class VenueElement {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long eventId;

    /**
     * stage=舞台，screen=屏幕，entrance=入口，exit=出口，other=其他自定义标注
     */
    private String type;

    private String label;

    private Integer posX;

    private Integer posY;

    private Integer width;

    private Integer height;

    private Integer rotation;

    private Integer sortOrder;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
