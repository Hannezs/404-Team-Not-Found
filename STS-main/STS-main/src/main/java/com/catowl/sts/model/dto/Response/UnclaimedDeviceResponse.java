package com.catowl.sts.model.dto.Response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "未认领的设备响应体（显示给用户）")
public class UnclaimedDeviceResponse {
    @Schema(description = "设备的 MAC 地址", example = "aabbcc112233")
    private String macAddress;

    @Schema(description = "设备首次注册上报的时间")
    private LocalDateTime registeredAt;

    @Schema(description = "一个易于识别的名称", example = "设备 (aabbcc)")
    private String friendlyName;
}
