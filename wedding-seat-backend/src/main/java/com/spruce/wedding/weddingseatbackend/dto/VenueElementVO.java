package com.spruce.wedding.weddingseatbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VenueElementVO {
    private Long id;
    private String type;
    private String label;
    private Integer posX;
    private Integer posY;
    private Integer width;
    private Integer height;
    private Integer rotation;
}
