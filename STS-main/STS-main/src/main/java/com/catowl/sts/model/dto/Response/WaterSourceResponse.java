package com.catowl.sts.model.dto.Response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "水源信息响应体")
public class WaterSourceResponse {
    @Schema(description = "水源的字符串ID", example = "ulid_source_xyz")
    private String strId;

    @Schema(description = "水源名称", example = "总部北侧排污口")
    private String sourceName;

    @Schema(description = "水源来源", example = "INDUSTRIAL")
    private String sourceType;

    @Schema(description = "绑定的 Dify 会话ID", example = "conv_123456789")
    private String difyConversationId;

    @Schema(description = "水源的详细描述", example = "主要负责处理生产废水")
    private String description;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "是否激活", example = "true")
    private Boolean isActive;
}
