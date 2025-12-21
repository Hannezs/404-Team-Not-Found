package com.catowl.sts.model.dto.Request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "设备注册请求体（来自传感器）")
public class DeviceRegisterRequest {
    @NotBlank
    @Schema(description = "设备的唯一 MAC 地址 (作为设备码)", example = "aabbcc112233")
    private String macAddress;
}
