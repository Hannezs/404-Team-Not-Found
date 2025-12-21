package com.catowl.sts.model.dto.Response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "报告发布状态更新响应")
public class ReportStatusUpdateResponse {
    @Schema(description = "报告的字符串ID", example = "ulid_report_abc")
    private String reportStrId;

    @Schema(description = "当前是否已发布", example = "true")
    private boolean isPublished;
}
