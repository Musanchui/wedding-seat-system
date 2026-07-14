package com.spruce.wedding.weddingseatbackend.controller.admin;

import com.spruce.wedding.weddingseatbackend.common.result.Result;
import com.spruce.wedding.weddingseatbackend.dto.AdminTableLayoutVO;
import com.spruce.wedding.weddingseatbackend.dto.AdminTableSaveDTO;
import com.spruce.wedding.weddingseatbackend.dto.AdminVenueLayoutVO;
import com.spruce.wedding.weddingseatbackend.service.AdminTableService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AdminTableController {

    private final AdminTableService adminTableService;

    /**
     * POST /api/admin/table/save
     * 新增/编辑桌位共用一个接口：请求体里 id 为空表示新增，有值表示编辑。
     * 新增时会自动按seatCount批量生成座位记录；编辑时如果seatCount改小了，
     * 会校验被缩减的座位有没有人坐，有人坐则拒绝缩减并报错提示。
     */
    @PostMapping("/admin/table/save")
    public Result<Void> saveTable(@RequestAttribute Long adminId, @Valid @RequestBody AdminTableSaveDTO dto) {
        adminTableService.saveTable(adminId, dto);
        return Result.success();
    }

    /**
     * DELETE /api/admin/table/{id}?force=false
     * 删除桌位。如果这桌还有来宾已经入座，默认会拒绝删除并提示；
     * 前端弹出二次确认后，带上 force=true 重新请求一次即可强制删除
     * (会自动清空这些来宾的座位信息，前端需要提醒管理员后续重新为这些来宾安排座位)
     */
    @DeleteMapping("/admin/table/{id}")
    public Result<Void> deleteTable(@RequestAttribute Long adminId, @PathVariable Long id,
                                     @RequestParam(defaultValue = "false") boolean force) {
        adminTableService.deleteTable(adminId, id, force);
        return Result.success();
    }

    /**
     * GET /api/admin/table/list?eventId=xxx
     * 某场婚礼下所有桌位详情(带每桌具体座位状态+来宾姓名手机号)
     */
    @GetMapping("/admin/table/list")
    public Result<List<AdminTableLayoutVO>> listTables(@RequestAttribute Long adminId,
                                                         @RequestParam Long eventId) {
        return Result.success(adminTableService.listTables(adminId, eventId));
    }

    /**
     * GET /api/admin/tables/layout?eventId=xxx
     * 管理端"桌位大地图"整页数据：画布尺寸+场地元素(舞台/屏幕/出入口)+每桌详细座位(带来宾信息)
     */
    @GetMapping("/admin/tables/layout")
    public Result<AdminVenueLayoutVO> getVenueLayout(@RequestAttribute Long adminId,
                                                      @RequestParam Long eventId) {
        return Result.success(adminTableService.getVenueLayout(adminId, eventId));
    }
}
