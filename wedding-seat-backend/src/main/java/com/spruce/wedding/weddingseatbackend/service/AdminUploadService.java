package com.spruce.wedding.weddingseatbackend.service;

import com.spruce.wedding.weddingseatbackend.dto.PhotoSortItemDTO;
import com.spruce.wedding.weddingseatbackend.dto.PhotoVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AdminUploadService {

    PhotoVO uploadPhoto(Long adminId, Long eventId, MultipartFile file);

    /** 上传背景音乐，成功后直接更新wedding_event.music_url，返回新的音乐URL */
    String uploadMusic(Long adminId, Long eventId, MultipartFile file);

    List<PhotoVO> listPhotos(Long adminId, Long eventId);

    void deletePhoto(Long adminId, Long photoId);

    void reorderPhotos(Long adminId, Long eventId, List<PhotoSortItemDTO> items);
}
