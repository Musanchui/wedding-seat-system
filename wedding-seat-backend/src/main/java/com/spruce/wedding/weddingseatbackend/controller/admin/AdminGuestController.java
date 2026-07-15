package com.spruce.wedding.weddingseatbackend.controller.admin;

import com.spruce.wedding.weddingseatbackend.common.result.Result;
import com.spruce.wedding.weddingseatbackend.dto.AdminGuestListItemVO;
import com.spruce.wedding.weddingseatbackend.service.AdminGuestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class AdminGuestController {

    private final AdminGuestService adminGuestService;

    /**
     * GET /api/admin/guest/list?eventId=xxx
     * 管理端展示宾客名单用（表格展示），导出按钮走下面的export接口
     */
    @GetMapping("/admin/guest/list")
    public Result<List<AdminGuestListItemVO>> listGuests(@RequestAttribute Long adminId,
                                                           @RequestParam Long eventId) {
        return Result.success(adminGuestService.listGuests(adminId, eventId));
    }

    /**
     * GET /api/admin/guest/export?eventId=xxx
     * 导出宾客名单Excel文件。注意这个接口不返回Result包装的JSON，直接返回文件字节流。
     */
    @GetMapping("/admin/guest/export")
    public ResponseEntity<byte[]> exportGuestList(@RequestAttribute Long adminId,
                                                   @RequestParam Long eventId) {
        byte[] excelBytes = adminGuestService.exportGuestListExcel(adminId, eventId);

        // 文件名含中文，用RFC 5987的filename*写法编码，避免部分浏览器/下载工具乱码
        String encodedFilename = URLEncoder.encode("宾客名单.xlsx", StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }
}
