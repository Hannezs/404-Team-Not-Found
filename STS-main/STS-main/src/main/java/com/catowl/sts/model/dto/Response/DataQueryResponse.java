package com.catowl.sts.model.dto.Response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "查询水质数据响应体")
public class DataQueryResponse {
    @Schema(description = "数据记录的字符串ID", example = "ulid_data_789")
    private String strId;

    @Schema(description = "浊度测量值", example = "150.75")
    private BigDecimal turbidityValue;

    @Schema(description = "测量单位", example = "NTU")
    private String unit;

    @Schema(description = "传感器的测量时间")
    private LocalDateTime measuredAt;
}
