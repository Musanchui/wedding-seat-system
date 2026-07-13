package com.spruce.wedding.weddingseatbackend.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class EventCreateDTO {

    private String groomName;

    private String brideName;

    /**
     * 访问短标识（如 zhang-li-0815）。不传的话后端自动生成一个随机的（管理员可以后续通过编辑接口改成好记的）。
     * 传的话必须是小写字母/数字/短横线，3-50位，且全局唯一（不同管理员之间也不能重复，因为这是URL的一部分）
     */
    @Pattern(regexp = "^[a-z0-9-]{3,50}$", message = "访问标识只能包含小写字母、数字和短横线，长度3-50位")
    private String slug;
}
