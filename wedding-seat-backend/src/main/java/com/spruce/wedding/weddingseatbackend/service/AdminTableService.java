package com.spruce.wedding.weddingseatbackend.service;

import com.spruce.wedding.weddingseatbackend.dto.AdminTableLayoutVO;
import com.spruce.wedding.weddingseatbackend.dto.AdminTableSaveDTO;
import com.spruce.wedding.weddingseatbackend.dto.AdminVenueLayoutVO;

import java.util.List;

public interface AdminTableService {

    void saveTable(Long adminId, AdminTableSaveDTO dto);

    void deleteTable(Long adminId, Long tableId, boolean force);

    /**
     * 管理端"桌位大地图"整页数据：画布+场地元素+每桌详细座位(带来宾姓名手机号)
     */
    AdminVenueLayoutVO getVenueLayout(Long adminId, Long eventId);

    List<AdminTableLayoutVO> listTables(Long adminId, Long eventId);
}
