package com.spruce.wedding.weddingseatbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("table_info")
public class TableInfo {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long eventId;

    private String tableNo;

    private Integer seatCount;

    private String remark;

    /**
     * 1=启用，0=停用
     */
    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
