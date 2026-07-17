package com.spruce.wedding.weddingseatbackend.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.spruce.wedding.weddingseatbackend.common.exception.BusinessException;
import com.spruce.wedding.weddingseatbackend.dto.PhotoSortItemDTO;
import com.spruce.wedding.weddingseatbackend.dto.PhotoVO;
import com.spruce.wedding.weddingseatbackend.entity.Photo;
import com.spruce.wedding.weddingseatbackend.entity.WeddingEvent;
import com.spruce.wedding.weddingseatbackend.mapper.PhotoMapper;
import com.spruce.wedding.weddingseatbackend.mapper.WeddingEventMapper;
import com.spruce.wedding.weddingseatbackend.service.AdminUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminUploadServiceImpl implements AdminUploadService {

    private final WeddingEventMapper weddingEventMapper;
    private final PhotoMapper photoMapper;

    @Value("${wedding.upload.base-path}")
    private String uploadBasePath;

    @Value("${wedding.upload.url-prefix}")
    private String uploadUrlPrefix;

    private static final Set<String> IMAGE_EXT = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> AUDIO_EXT = Set.of("mp3", "wav", "m4a");

    @Override
    public PhotoVO uploadPhoto(Long adminId, Long eventId, MultipartFile file) {
        getOwnedEvent(adminId, eventId);

        String ext = getExtension(file);
        if (!IMAGE_EXT.contains(ext)) {
            throw new BusinessException("只支持 jpg/jpeg/png/webp 格式的图片");
        }

        String relativePath = "photo/" + eventId + "/" + UUID.randomUUID() + "." + ext;
        saveFileToDisk(file, relativePath);

        Integer maxSort = photoMapper.selectList(
                        Wrappers.<Photo>lambdaQuery().eq(Photo::getEventId, eventId).orderByDesc(Photo::getSortOrder).last("LIMIT 1"))
                .stream().map(Photo::getSortOrder).findFirst().orElse(-1);

        Photo photo = new Photo();
        photo.setEventId(eventId);
        photo.setUrl(uploadUrlPrefix + relativePath);
        photo.setSortOrder(maxSort + 1);
        photoMapper.insert(photo);

        return new PhotoVO(photo.getId(), photo.getUrl(), photo.getSortOrder());
    }

    @Override
    public String uploadMusic(Long adminId, Long eventId, MultipartFile file) {
        WeddingEvent event = getOwnedEvent(adminId, eventId);

        String ext = getExtension(file);
        if (!AUDIO_EXT.contains(ext)) {
            throw new BusinessException("只支持 mp3/wav/m4a 格式的音频");
        }

        // 如果之前传过音乐，把旧文件删掉，避免磁盘上堆积没用的文件
        if (event.getMusicUrl() != null && event.getMusicUrl().startsWith(uploadUrlPrefix)) {
            deletePhysicalFile(event.getMusicUrl());
        }

        String relativePath = "music/" + eventId + "/" + UUID.randomUUID() + "." + ext;
        saveFileToDisk(file, relativePath);

        String newUrl = uploadUrlPrefix + relativePath;
        event.setMusicUrl(newUrl);
        weddingEventMapper.updateById(event);
        return newUrl;
    }

    @Override
    public List<PhotoVO> listPhotos(Long adminId, Long eventId) {
        getOwnedEvent(adminId, eventId);
        List<Photo> photos = photoMapper.selectList(
                Wrappers.<Photo>lambdaQuery().eq(Photo::getEventId, eventId).orderByAsc(Photo::getSortOrder)
        );
        return photos.stream().map(p -> new PhotoVO(p.getId(), p.getUrl(), p.getSortOrder())).toList();
    }

    @Override
    public void deletePhoto(Long adminId, Long photoId) {
        Photo photo = photoMapper.selectById(photoId);
        if (photo == null) {
            throw new BusinessException("照片不存在");
        }
        getOwnedEvent(adminId, photo.getEventId());

        deletePhysicalFile(photo.getUrl());
        photoMapper.deleteById(photoId);
    }

    @Override
    public void reorderPhotos(Long adminId, Long eventId, List<PhotoSortItemDTO> items) {
        getOwnedEvent(adminId, eventId);
        for (PhotoSortItemDTO item : items) {
            Photo photo = photoMapper.selectById(item.getId());
            if (photo != null && photo.getEventId().equals(eventId)) {
                photo.setSortOrder(item.getSortOrder());
                photoMapper.updateById(photo);
            }
        }
    }

    // ============================================
    // 私有工具方法
    // ============================================

    private void saveFileToDisk(MultipartFile file, String relativePath) {
        try {
            Path targetPath = Path.of(uploadBasePath, relativePath);
            Files.createDirectories(targetPath.getParent());
            file.transferTo(targetPath);
        } catch (IOException e) {
            throw new BusinessException(500, "文件保存失败，请检查服务器磁盘路径配置");
        }
    }

    private void deletePhysicalFile(String url) {
        if (url == null || !url.startsWith(uploadUrlPrefix)) {
            return;
        }
        String relativePath = url.substring(uploadUrlPrefix.length());
        File file = new File(uploadBasePath, relativePath);
        if (file.exists()) {
            file.delete(); // 删除失败也不影响主流程(不常见，比如文件被占用)，不用抛异常打断数据库操作
        }
    }

    private String getExtension(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.contains(".")) {
            throw new BusinessException("文件名不合法");
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    private WeddingEvent getOwnedEvent(Long adminId, Long eventId) {
        WeddingEvent event = weddingEventMapper.selectById(eventId);
        if (event == null) {
            throw new BusinessException("婚礼不存在");
        }
        if (!event.getAdminId().equals(adminId)) {
            throw new BusinessException(403, "无权操作该婚礼");
        }
        return event;
    }
}
