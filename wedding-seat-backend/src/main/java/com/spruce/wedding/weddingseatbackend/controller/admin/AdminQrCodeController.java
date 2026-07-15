package com.spruce.wedding.weddingseatbackend.controller.admin;

import com.spruce.wedding.weddingseatbackend.service.AdminQrCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminQrCodeController {

    private final AdminQrCodeService adminQrCodeService;

    /**
     * GET /api/admin/event/qrcode?eventId=xxx
     * 生成邀请函二维码图片(PNG)。直接返回图片字节流，前端可以直接把这个接口地址当img的src用，
     * 或者调这个接口拿到blob后触发下载。
     * 加了 no-cache，避免管理员改了新人姓名/照片后，浏览器还在用旧的缓存图片。
     */
    @GetMapping(value = "/admin/event/qrcode", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getInvitationQrCode(@RequestAttribute Long adminId,
                                                       @RequestParam Long eventId) {
        byte[] imageBytes = adminQrCodeService.generateInvitationImage(adminId, eventId);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .contentType(MediaType.IMAGE_PNG)
                .body(imageBytes);
    }
}
