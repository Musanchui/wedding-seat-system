package com.spruce.wedding.weddingseatbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.spruce.wedding.weddingseatbackend.entity.Seat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SeatMapper extends BaseMapper<Seat> {

    /**
     * 抢座核心SQL：严格按照契约要求的乐观锁写法。
     * WHERE条件同时带上 id 和 version，只有当前version和前端传来的一致时才会更新成功。
     * 返回值是"受影响行数"——这是判断抢座是否成功的唯一依据：
     *   返回1 = 抢座成功
     *   返回0 = 版本不一致（要么座位已被别人抢先占用，要么前端本地缓存的version已经过期）
     */
    @Update("UPDATE seat SET guest_id = #{guestId}, status = 1, version = version + 1, " +
            "updated_at = NOW() WHERE id = #{seatId} AND version = #{version} AND status = 0")
    int lockSeat(@Param("seatId") Long seatId, @Param("guestId") Long guestId, @Param("version") Integer version);

    /**
     * 释放座位（用于"换座"场景：先把来宾原来占用的座位释放为空闲，再去抢新座位）
     */
    @Update("UPDATE seat SET guest_id = NULL, status = 0, version = version + 1, " +
            "updated_at = NOW() WHERE id = #{seatId} AND guest_id = #{guestId}")
    int releaseSeat(@Param("seatId") Long seatId, @Param("guestId") Long guestId);
}
