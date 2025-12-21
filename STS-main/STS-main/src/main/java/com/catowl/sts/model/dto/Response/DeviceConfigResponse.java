package com.catowl.sts.model.dto.Response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "设备配置响应体（发给传感器）")
public class DeviceConfigResponse {
    @Schema(description = "为该设备生成的唯一 API Key", example = "dev_key_abc123xyz789")
    private String deviceApiKey;

    @Schema(description = "设备应绑定的水源 strId")
    private String sourceStrId;
}
