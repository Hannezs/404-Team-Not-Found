package com.catowl.sts.controller;

import com.catowl.sts.model.dto.Request.ReportGenerateRequest;
import com.catowl.sts.model.dto.Response.*;
import com.catowl.sts.model.entity.User;
import com.catowl.sts.service.ReportService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@Api(tags = "4. 分析报告 (私有)", description = "管理用户的分析报告、生成、发布和删除")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @PostMapping("/generate")
    @ApiOperation(value = "生成新报告", notes = "请求 Dify 工作流为指定水源生成新报告")
    @ApiResponses({
            @io.swagger.annotations.ApiResponse(code = 201, message = "报告生成成功")
    })
    public ResponseEntity<MyApiResponse<ReportResponse>> generateReport(
            @Valid @RequestBody ReportGenerateRequest generateRequest) {
        ReportResponse reportResponse = reportService.generateReport(generateRequest);
        MyApiResponse<ReportResponse> response = new MyApiResponse<>("报告生成成功", reportResponse);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    @ApiOperation(value = "获取当前用户的所有报告", notes = "列出当前用户的所有报告 (包括未发布的)")
    public ResponseEntity<MyApiResponse<List<ReportTagResponse>>> getAllReports(
            @ApiParam(value = "上一页最后一条记录的 strId (首页查询则不传)", example = "report_ulid_mock_page1_item2")
            @RequestParam(required = false) String lastStrId,
            @ApiParam(value = "每页大小", example = "10")
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        List<ReportTagResponse> reports = reportService.getReportsForUser(user.getId(), lastStrId, pageSize);
        return ResponseEntity.ok(new MyApiResponse<>("获取成功", reports));
    }

    @GetMapping("/{reportStrId}")
    @ApiOperation(value = "获取特定报告详情 (私有)", notes = "获取报告详情，无论是否发布 (需要所有权)")
    public ResponseEntity<MyApiResponse<ReportDetailResponse>> getReportById(
            @ApiParam(value = "报告的字符串ID", example = "report_ulid_mock_qwe")
            @PathVariable String reportStrId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        ReportDetailResponse report = reportService.getReportByIdForUser(reportStrId,user.getId());
        return ResponseEntity.ok(new MyApiResponse<>("获取成功", report));
    }

    @GetMapping("/by-source/{sourceStrId}")
    @ApiOperation(value = "根据水源ID获取报告列表", notes = "只获取报告id以及报告关键字，供用户选择，具体查询请调用/{reportStrId}")
    public ResponseEntity<MyApiResponse<List<ReportTagResponse>>> getReportsBySourceStrId(
            @ApiParam(value = "水源的字符串ID", example = "source_ulid_mock_page1_item2")
            @PathVariable String sourceStrId,
            @ApiParam(value = "上一页最后一条记录的 strId (首页查询则不传)", example = "report_ulid_mock_page1_item2")
            @RequestParam(required = false) String lastStrId,
            @ApiParam(value = "每页大小（不填就是默认为10）", example = "10")
            @RequestParam(defaultValue = "10") int pageSize){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        List<ReportTagResponse> response = reportService.getReportTagsBySourceId(sourceStrId,user.getId(),lastStrId,pageSize);
        return ResponseEntity.ok(new MyApiResponse<>("获取报告列表成功",response));
    }

    @DeleteMapping("/{reportStrId}")
    @ApiOperation(value = "删除报告", notes = "删除一份分析报告")
    public ResponseEntity<MyApiResponse<String>> deleteReport(
            @ApiParam(value = "报告的字符串ID", example = "report_ulid_mock_qwe")
            @PathVariable String reportStrId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        reportService.deleteReport(reportStrId, user.getId());
        return ResponseEntity.ok(new MyApiResponse<>("删除成功", null));
    }

    @PostMapping("/{reportStrId}/publish")
    @ApiOperation(value = "发布报告", notes = "将报告设为公开可见 (二维码将生效)")
    public ResponseEntity<MyApiResponse<ReportStatusUpdateResponse>> publishReport(
            @ApiParam(value = "报告的字符串ID", example = "report_ulid_mock_qwe")
            @PathVariable String reportStrId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        ReportStatusUpdateResponse response = reportService.publishReport(reportStrId, user.getId());
        return ResponseEntity.ok(new MyApiResponse<>("报告已发布", response));
    }

    @PostMapping("/{reportStrId}/unpublish")
    @ApiOperation(value = "取消发布报告", notes = "将报告设为私有 (二维码将失效)")
    public ResponseEntity<MyApiResponse<ReportStatusUpdateResponse>> unpublishReport(
            @ApiParam(value = "报告的字符串ID", example = "report_ulid_mock_qwe")
            @PathVariable String reportStrId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        ReportStatusUpdateResponse response = reportService.unpublishReport(reportStrId, user.getId());
        return ResponseEntity.ok(new MyApiResponse<>("报告已设为私有", response));
    }
}
