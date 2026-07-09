package com.spruce.wedding.weddingseatbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("seat")
public class Seat {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tableId;

    private Integer seatNo;

    /**
     * 0=空闲，1=已占用
     */
    private Integer status;

    private Long guestId;

    /**
     * 乐观锁版本号。加了@Version注解后，MyBatis-Plus的updateById()方法会自动：
     * 1. UPDATE语句的WHERE条件里带上 version = 当前值
     * 2. SET子句里自动 version = version + 1
     * 3. 如果影响行数为0（说明version已被别人改过），MyBatis-Plus本身不会抛异常，需要业务代码自行判断返回值
     * 但本项目前端会自己传version过来做校验（见GuestServiceImpl的锁座逻辑），所以这里的@Version
     * 主要是为了让MybatisPlusInterceptor里的乐观锁插件生效，双重保险。
     */
    @Version
    private Integer version;

    private LocalDateTime updatedAt;
}
