package com.zipe.base.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author gary.tsai 2019/5/31
 */
@Data
public class Base {
    // 更新人員
    private String updatedBy;
    // 更新時間
    private LocalDateTime updatedAt;
    // 建立人員
    private String createdBy;
    // 建立時間
    private LocalDateTime createdAt;
}
