package com.catowl.sts.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportDetailVO {
    // 报告ID (Long)，用于查询关联数据
    private Long id;

    // 报告表 (analysis_reports)
    private String strId;
    private String reportContent;
    private String qrCodePath;
    private LocalDateTime generatedAt;
    private String difyMessageId;
    private String keywords;
    private boolean isPublished;

    // 水源表 (water_sources)
    private String sourceName;
    private String sourceType;
    private String sourceStrId;

    // 用户表 (users)
    private String username;
}