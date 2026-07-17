package com.spruce.wedding.weddingseatbackend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PhotoSortItemDTO {

    @NotNull
    private Long id;

    @NotNull
    private Integer sortOrder;
}
