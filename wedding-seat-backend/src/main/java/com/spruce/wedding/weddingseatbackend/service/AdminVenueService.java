package com.spruce.wedding.weddingseatbackend.service;

import com.spruce.wedding.weddingseatbackend.dto.AdminVenueElementSaveDTO;

public interface AdminVenueService {

    void saveElement(Long adminId, AdminVenueElementSaveDTO dto);

    void deleteElement(Long adminId, Long elementId);
}
