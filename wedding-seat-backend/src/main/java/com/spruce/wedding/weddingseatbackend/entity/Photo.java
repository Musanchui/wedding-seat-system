package com.spruce.wedding.weddingseatbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("photo") // 注意：实际数据库表名是 photo，不是 wedding_photo
public class Photo {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long eventId;

    private String url;

    private Integer sortOrder;

    private LocalDateTime createdAt;
}
