package com.catowl.sts.model.dto.Request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "更新水源信息请求体")
public class WaterSourceUpdateRequest {
    @Schema(description = "新的水源名称", example = "总部北侧排污口(A)")
    private String sourceName;

    @Schema(description = "新的详细描述", example = "主要负责处理A栋生产废水")
    private String description;

    @Schema(description = "是否激活该监测点", example = "true")
    private Boolean isActive;
}
