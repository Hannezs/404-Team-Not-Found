package com.catowl.sts.model.dto.Request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "设备配置响应体（发给传感器）")
public class DevicePairRequest {
    @NotBlank
    @Schema(description = "要绑定的设备 MAC 地址", example = "aabbcc112233")
    private String macAddress;

    @NotBlank
    @Schema(description = "要绑定到的水源 strId", example = "ulid_source_abc_123")
    private String sourceStrId;
}
