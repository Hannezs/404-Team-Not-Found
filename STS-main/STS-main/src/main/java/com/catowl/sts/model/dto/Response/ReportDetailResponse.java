package com.catowl.sts.model.dto.Response;

import com.catowl.sts.model.vo.ReportDetailVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "详细分析报告响应体 (私有)")
public class ReportDetailResponse {

    @Schema(description = "报告的字符串ID", example = "ulid_report_abc")
    private String strId;

    @Schema(description = "Dify 生成的报告全文 (Markdown 或 Text)")
    private String reportContent;

    @Schema(description = "报告二维码的存储路径或 URL", example = "/static/qr/ulid_report_abc.png")
    private String qrCodePath;

    @Schema(description = "报告生成时间")
    private LocalDateTime generatedAt;

    @Schema(description = "Dify 返回的消息ID", example = "msg_12345")
    private String difyMessageId;

    @Schema(description = "报告提取的关键词", example = "浊度, 异常, 建议")
    private String keywords;

    @Schema(description = "是否已发布 (公开)", example = "false")
    private Boolean isPublished;

    // --- 新增关联信息 ---
    @Schema(description = "水源名称", example = "总部北侧排污口")
    private String sourceName;

    @Schema(description = "水源来源", example = "INDUSTRIAL")
    private String sourceType;

    @Schema(description = "水源字符串ID")
    private String sourceStrId;

    @Schema(description = "报告所有者用户名", example = "admin")
    private String username;

    @Schema(description = "用于生成该报告的水质数据列表")
    private List<DataQueryResponse> usedData;

    /**
     * 辅助方法，用于从 VO 转换
     */
    public void setFromVO(ReportDetailVO vo) {
        if (vo == null) return;
        this.strId = vo.getStrId();
        this.reportContent = vo.getReportContent();
        this.qrCodePath = vo.getQrCodePath();
        this.generatedAt = vo.getGeneratedAt();
        this.difyMessageId = vo.getDifyMessageId();
        this.keywords = vo.getKeywords();
        this.isPublished = vo.isPublished();
        this.sourceName = vo.getSourceName();
        this.sourceType = vo.getSourceType();
        this.username = vo.getUsername();
        this.sourceStrId = vo.getSourceStrId();
    }
}