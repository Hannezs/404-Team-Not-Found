package com.catowl.sts.model.dto.Response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "分析报告响应体")
public class ReportResponse {
    @Schema(description = "报告的字符串ID", example = "ulid_report_abc")
    private String strId;

    @Schema(description = "关联的水源字符串ID (如果水源被删除, 可能为 null)", example = "ulid_source_xyz")
    private String sourceStrId;

    @Schema(description = "Dify 生成的报告全文 (Markdown 或 Text)")
    private String reportContent;

    @Schema(description = "报告二维码的存储路径或 URL", example = "/static/qr/ulid_report_abc.png")
    private String qrCodePath;

    @Schema(description = "报告生成时间")
    private LocalDateTime generatedAt;

    @Schema(description = "Dify 返回的消息ID", example = "msg_12345")
    private String difyMessageId;

    @Schema(description = "报告提取的关键词", example = "浊度，异常，建议")
    private String keywords;

    @Schema(description = "是否已发布 (公开)", example = "false")
    private Boolean isPublished;
}
