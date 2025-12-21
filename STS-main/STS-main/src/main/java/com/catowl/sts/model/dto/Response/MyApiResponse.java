package com.catowl.sts.model.dto.Response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "通用的 API 成功响应包装")
public class MyApiResponse<T>{
    @Schema(description = "操作结果消息", example = "操作成功")
    private String message;

    @Schema(description = "返回的数据")
    private T data;
}
