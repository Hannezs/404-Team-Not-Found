package com.catowl.sts.controller;

import com.catowl.sts.model.dto.Response.MyApiResponse;
import com.catowl.sts.model.dto.Response.ReportPublicResponse;
import com.catowl.sts.service.PublicReportService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponses;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/public/reports")
@Api(tags = "5. 分析报告 (公开)", description = "供任何人 (例如通过二维码) 查看已发布的报告")
public class PublicReportController {

    @Autowired
    private PublicReportService publicReportService;

    @GetMapping("/{reportStrId}")
    @ApiOperation(value = "查看公开报告", notes = "根据 strId 获取已发布的报告详情 (无需认证)")
    @ApiResponses({
            @io.swagger.annotations.ApiResponse(code = 200, message = "获取成功"),
            @io.swagger.annotations.ApiResponse(code = 404, message = "报告未找到或未发布")
    })
    public ResponseEntity<MyApiResponse<ReportPublicResponse>> getPublicReport(
            @ApiParam(value = "报告的字符串ID", example = "report_ulid_mock_qwe")
            @PathVariable String reportStrId) {
        ReportPublicResponse report = publicReportService.getPublicReport(reportStrId);
        MyApiResponse<ReportPublicResponse> response = new MyApiResponse<>("获取成功", report);
        return ResponseEntity.ok(response);
    }
}
