package com.catowl.sts.model.dto.Request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@Schema(description = "生成新报告请求体")
public class ReportGenerateRequest {
    @NotBlank
    @Schema(description = "需要分析的水源字符串ID", example = "ulid_source_xyz", required = true)
    private String sourceStrId;
}
