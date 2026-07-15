package com.spruce.wedding.weddingseatbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminGuestListItemVO {
    private Long id;

    /**
     * 展示用名字：只占1个座位就是原名；占多个座位就是"张三及其家人2位"这种格式
     */
    private String displayName;

    private String name;
    private String phone;
    private String category;

    /** 座位描述，如 "3号桌-2号、3号桌-5号"，没选座位则是空字符串 */
    private String seatsDesc;

    private Integer seatCount;
    private LocalDateTime registerTime;
}
