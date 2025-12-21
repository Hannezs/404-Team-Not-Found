package com.catowl.sts.model.dto.Response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "公开分析报告响应体 (用于二维码扫描等公开访问)")
public class ReportPublicResponse {
    @Schema(description = "报告的字符串ID", example = "ulid_report_abc")
    private String strId;

    @Schema(description = "水源名称 (提供上下文, Service 层需要额外查询)", example = "总部北侧排污口")
    private String sourceName;

    @Schema(description = "水源来源 (提供上下文, Service 层需要额外查询)", example = "INDUSTRIAL")
    private String sourceType;

    @Schema(description = "Dify 生成的报告全文 (Markdown 或 Text)")
    private String reportContent;

    @Schema(description = "报告提取的关键词", example = "浊度，异常，建议")
    private String keywords;

    @Schema(description = "二维码的存储路径")
    private String qrCodePath;

    @Schema(description = "报告生成时间")
    private LocalDateTime generatedAt;
}
