package com.spruce.wedding.weddingseatbackend.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPlusConfig {

    /**
     * 注册乐观锁插件：这样以后任何地方用 xxxMapper.updateById(entity) 更新带 @Version 字段的实体，
     * MyBatis-Plus会自动在SQL里带上 version 校验条件。
     * 注意：本项目选座这条关键路径用的是SeatMapper.lockSeat()这条手写SQL，已经手动实现了同样的乐观锁逻辑，
     * 不依赖这个插件也能正常工作；这里注册插件主要是为了让Seat实体上的@Version注解在其他调用场景
     * （比如以后管理端如果用updateById更新座位）下也能生效，双重保险。
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return interceptor;
    }
}
