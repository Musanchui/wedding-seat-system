package com.spruce.wedding.weddingseatbackend.controller.admin;

import com.spruce.wedding.weddingseatbackend.common.result.Result;
import com.spruce.wedding.weddingseatbackend.dto.AdminVenueElementSaveDTO;
import com.spruce.wedding.weddingseatbackend.service.AdminVenueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminVenueController {

    private final AdminVenueService adminVenueService;

    /**
     * POST /api/admin/venue-element/save
     * 新增/编辑场地元素(舞台/屏幕/入口/出口)共用一个接口，id为空表示新增
     */
    @PostMapping("/admin/venue-element/save")
    public Result<Void> saveElement(@RequestAttribute Long adminId, @Valid @RequestBody AdminVenueElementSaveDTO dto) {
        adminVenueService.saveElement(adminId, dto);
        return Result.success();
    }

    /**
     * DELETE /api/admin/venue-element/{id}
     */
    @DeleteMapping("/admin/venue-element/{id}")
    public Result<Void> deleteElement(@RequestAttribute Long adminId, @PathVariable Long id) {
        adminVenueService.deleteElement(adminId, id);
        return Result.success();
    }
}
