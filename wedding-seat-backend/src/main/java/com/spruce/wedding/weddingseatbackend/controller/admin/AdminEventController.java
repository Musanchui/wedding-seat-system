package com.spruce.wedding.weddingseatbackend.controller.admin;

import com.spruce.wedding.weddingseatbackend.common.result.Result;
import com.spruce.wedding.weddingseatbackend.dto.EventCreateDTO;
import com.spruce.wedding.weddingseatbackend.dto.EventDetailVO;
import com.spruce.wedding.weddingseatbackend.dto.EventListItemVO;
import com.spruce.wedding.weddingseatbackend.dto.EventUpdateDTO;
import com.spruce.wedding.weddingseatbackend.service.AdminEventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 婚礼实例管理接口，全部需要登录(JwtInterceptor会拦截校验token)。
 * adminId 参数由 JwtInterceptor 解析token后存入request属性，这里直接用 @RequestAttribute 取，
 * 不需要前端传，也不需要Controller自己解析token。
 */
@RestController
@RequestMapping("/admin/event")
@RequiredArgsConstructor
public class AdminEventController {

    private final AdminEventService adminEventService;

    /**
     * POST /api/admin/event
     * 创建一场新婚礼，创建后默认是"筹备中"状态，需要管理员编辑完信息后手动发布
     */
    @PostMapping
    public Result<EventDetailVO> create(@RequestAttribute Long adminId, @Valid @RequestBody EventCreateDTO dto) {
        return Result.success(adminEventService.create(adminId, dto));
    }

    /**
     * PUT /api/admin/event/{id}
     * 编辑婚礼信息，只有字段传了值才会更新（null表示不修改该字段，不是"清空"）
     */
    @PutMapping("/{id}")
    public Result<Void> update(@RequestAttribute Long adminId, @PathVariable Long id,
                                @Valid @RequestBody EventUpdateDTO dto) {
        adminEventService.update(adminId, id, dto);
        return Result.success();
    }

    /**
     * GET /api/admin/event/{id}
     * 查询单场婚礼详情，会校验这场婚礼是不是当前登录管理员名下的
     */
    @GetMapping("/{id}")
    public Result<EventDetailVO> getDetail(@RequestAttribute Long adminId, @PathVariable Long id) {
        return Result.success(adminEventService.getDetail(adminId, id));
    }

    /**
     * GET /api/admin/event/list
     * 查询当前登录管理员名下所有婚礼列表
     */
    @GetMapping("/list")
    public Result<List<EventListItemVO>> listMyEvents(@RequestAttribute Long adminId) {
        return Result.success(adminEventService.listMyEvents(adminId));
    }
}
