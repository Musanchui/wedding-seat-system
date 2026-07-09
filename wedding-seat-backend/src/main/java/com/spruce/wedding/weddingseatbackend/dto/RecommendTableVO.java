package com.spruce.wedding.weddingseatbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecommendTableVO {
    private Long id;
    private String tableNo;
    private String remark;
    private Integer availableSeatsCount;
}
