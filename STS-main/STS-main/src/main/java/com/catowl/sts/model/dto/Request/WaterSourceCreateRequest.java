package com.catowl.sts.model.dto.Request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "创建新水源（聊天）请求体")
public class WaterSourceCreateRequest {
    @NotBlank
    @Schema(description = "水源名称", example = "总部北侧排污口", required = true)
    private String sourceName;

    @Schema(description = "水源来源 (AGRICULTURAL, INDUSTRIAL, DOMESTIC等)", example = "INDUSTRIAL", required = true)
    private String sourceType;

    @Schema(description = "水源的详细描述", example = "主要负责处理生产废水")
    private String description;
}
