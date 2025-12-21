package com.catowl.sts.model.dto.Response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "上传水质数据响应体")
public class DataUploadResponse {
    @Schema(description = "处理状态", example = "success")
    private String status;

    @Schema(description = "新创建的数据记录ID", example = "ulid_data_789")
    private String dataStrId;
}
