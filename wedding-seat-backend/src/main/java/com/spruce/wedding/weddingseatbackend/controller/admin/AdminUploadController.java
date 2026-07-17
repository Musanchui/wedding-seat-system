package com.spruce.wedding.weddingseatbackend.controller.admin;

import com.spruce.wedding.weddingseatbackend.common.result.Result;
import com.spruce.wedding.weddingseatbackend.dto.PhotoSortItemDTO;
import com.spruce.wedding.weddingseatbackend.dto.PhotoVO;
import com.spruce.wedding.weddingseatbackend.service.AdminUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AdminUploadController {

    private final AdminUploadService adminUploadService;

    /**
     * POST /api/admin/upload/photo
     * Form-Data: file(图片文件) + eventId
     */
    @PostMapping("/admin/upload/photo")
    public Result<PhotoVO> uploadPhoto(@RequestAttribute Long adminId,
                                        @RequestParam Long eventId,
                                        @RequestPart MultipartFile file) {
        return Result.success(adminUploadService.uploadPhoto(adminId, eventId, file));
    }

    /**
     * POST /api/admin/upload/music
     * Form-Data: file(音频文件) + eventId
     * 上传成功会自动更新这场婚礼的music_url，不需要再单独调编辑接口
     */
    @PostMapping("/admin/upload/music")
    public Result<String> uploadMusic(@RequestAttribute Long adminId,
                                       @RequestParam Long eventId,
                                       @RequestPart MultipartFile file) {
        return Result.success(adminUploadService.uploadMusic(adminId, eventId, file));
    }

    /**
     * GET /api/admin/photo/list?eventId=xxx
     * 管理端查看当前已上传的照片列表(用于编辑页展示、删除、排序)
     */
    @GetMapping("/admin/photo/list")
    public Result<List<PhotoVO>> listPhotos(@RequestAttribute Long adminId, @RequestParam Long eventId) {
        return Result.success(adminUploadService.listPhotos(adminId, eventId));
    }

    /**
     * DELETE /api/admin/photo/{id}
     */
    @DeleteMapping("/admin/photo/{id}")
    public Result<Void> deletePhoto(@RequestAttribute Long adminId, @PathVariable Long id) {
        adminUploadService.deletePhoto(adminId, id);
        return Result.success();
    }

    /**
     * PUT /api/admin/photo/sort
     * 批量更新照片排序号：请求体传 { eventId, items: [{id, sortOrder}, ...] }
     */
    @PutMapping("/admin/photo/sort")
    public Result<Void> reorderPhotos(@RequestAttribute Long adminId, @RequestBody PhotoSortRequest request) {
        adminUploadService.reorderPhotos(adminId, request.getEventId(), request.getItems());
        return Result.success();
    }

    /** 排序接口的请求体，单独定义一个内部类，避免为了一个简单的List包装再多建一个文件 */
    public static class PhotoSortRequest {
        private Long eventId;
        private List<PhotoSortItemDTO> items;

        public Long getEventId() { return eventId; }
        public void setEventId(Long eventId) { this.eventId = eventId; }
        public List<PhotoSortItemDTO> getItems() { return items; }
        public void setItems(List<PhotoSortItemDTO> items) { this.items = items; }
    }
}
