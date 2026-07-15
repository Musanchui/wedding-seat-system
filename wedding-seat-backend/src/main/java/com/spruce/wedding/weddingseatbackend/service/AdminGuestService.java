package com.spruce.wedding.weddingseatbackend.service;

import com.spruce.wedding.weddingseatbackend.dto.AdminGuestListItemVO;

import java.util.List;

public interface AdminGuestService {

    List<AdminGuestListItemVO> listGuests(Long adminId, Long eventId);

    /** 生成宾客名单Excel文件的字节内容，供Controller包装成下载响应 */
    byte[] exportGuestListExcel(Long adminId, Long eventId);
}
