package com.catowl.sts.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WaterQualityData implements Serializable {
    private Long id;
    private String strId;
    private Long sourceId; // 扁平化外键
    private BigDecimal turbidityValue;
    private String unit;
    private LocalDateTime measuredAt;
    private LocalDateTime uploadedAt;
}
