package com.catowl.sts.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisReport implements Serializable {
    private Long id;
    private String strId;
    private Long sourceId; // 扁平化外键, 允许为 NULL
    private String reportContent;
    private String qrCodePath;
    private LocalDateTime generatedAt;
    private String difyMessageId;
    private String keywords;
    private String sourceStrId;
    private boolean isPublished; // (新增) 对应数据库 is_published 字段
}
