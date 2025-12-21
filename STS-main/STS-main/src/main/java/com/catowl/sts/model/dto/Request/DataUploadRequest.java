package com.catowl.sts.model.dto.Request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "上传水质数据请求体 (来自传感器)")
public class DataUploadRequest {
    @NotBlank
    @Schema(description = "目标水源的字符串ID", example = "ulid_source_xyz", required = true)
    private String sourceStrId;

    @NotNull
    @DecimalMin("0.0")
    @Schema(description = "浊度测量值", example = "150.75", required = true)
    private BigDecimal turbidityValue;

    @NotBlank
    @Schema(description = "测量单位", example = "NTU", required = true)
    private String unit;

    @NotNull
    @Schema(description = "传感器的测量时间 (ISO 8601 格式)", example = "2025-10-30T10:30:00", required = true)
    private LocalDateTime measuredAt;
}
