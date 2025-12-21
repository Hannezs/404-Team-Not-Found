package com.catowl.sts.model.dto.Response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "报告简讯响应体")
public class ReportTagResponse {
    @Schema(description = "报告的字符串ID", example = "ulid_report_abc")
    private String strId;

    @Schema(description = "报告的关键词", example = "气候,泥沙,杂质,底泥,藻类")
    private String keywords;

    @Schema(description = "报告生成的时间")
    private LocalDateTime generatedAt;
}
