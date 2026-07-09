package com.spruce.wedding.weddingseatbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.spruce.wedding.weddingseatbackend.entity.Guest;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GuestMapper extends BaseMapper<Guest> {
}
