package com.catowl.sts.controller;

import com.catowl.sts.model.dto.Request.WaterSourceCreateRequest;
import com.catowl.sts.model.dto.Request.WaterSourceUpdateRequest;
import com.catowl.sts.model.dto.Response.MyApiResponse;
import com.catowl.sts.model.dto.Response.WaterSourceResponse;
import com.catowl.sts.model.entity.User;
import com.catowl.sts.service.WaterSourceService;
import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/water-sources")
@Api(tags = "2. 水源信息 (Dify聊天)", description = "管理用户的水源监测点 (即 Dify 聊天会话)")
public class WaterSourceController {
    @Autowired
    private WaterSourceService waterSourceService;

    @PostMapping
    @ApiOperation(value = "创建新水源 (聊天)", notes = "创建一个新的水源监测点，并初始化 Dify 会话")
    @ApiResponses({
            @ApiResponse(code = 201, message = "创建成功")
    })
    public ResponseEntity<MyApiResponse<WaterSourceResponse>> createWaterSource(
            @Valid @RequestBody WaterSourceCreateRequest createRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        WaterSourceResponse response = waterSourceService.createWaterSource(createRequest, user);
        return new ResponseEntity<>(new MyApiResponse<>("创建成功", response), HttpStatus.CREATED);
    }

    @GetMapping
    @ApiOperation(value = "分页获取当前用户的水源", notes = "使用游标分页 (cursor pagination) 列出当前用户的所有水源监测点")
    public ResponseEntity<MyApiResponse<List<WaterSourceResponse>>> getAllWaterSources(
            @ApiParam(value = "上一页最后一条记录的 strId (首页查询则不传)", example = "source_ulid_mock_page1_item2")
            @RequestParam(required = false) String lastStrId,
            @ApiParam(value = "每页大小", example = "10")
            @RequestParam(defaultValue = "10") int pageSize,
            @ApiParam(value = "水源名称模糊查询关键词（可不填）", example = "总部")
            @RequestParam(required = false) String searchName) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        List<WaterSourceResponse> response = waterSourceService.getAllWaterSources(user.getId(), lastStrId, pageSize, searchName);
        return ResponseEntity.ok(new MyApiResponse<>("获取成功", response));
    }

    @GetMapping("/{sourceStrId}")
    @ApiOperation(value = "获取特定水源详情", notes = "根据 strId 获取单个水源的详细信息")
    public ResponseEntity<MyApiResponse<WaterSourceResponse>> getWaterSourceById(
            @ApiParam(value = "水源的字符串ID", example = "source_ulid_mock_abc")
            @PathVariable String sourceStrId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        WaterSourceResponse response = waterSourceService.getWaterSource(sourceStrId, user.getId());
        return ResponseEntity.ok(new MyApiResponse<>("获取成功",response));
    }

    @PutMapping("/{sourceStrId}")
    @ApiOperation(value = "更新水源信息", notes = "更新特定水源的名称、描述或激活状态")
    public ResponseEntity<MyApiResponse<WaterSourceResponse>> updateWaterSource(
            @ApiParam(value = "水源的字符串ID", example = "source_ulid_mock_abc")
            @PathVariable String sourceStrId,
            @Valid @RequestBody WaterSourceUpdateRequest updateRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        WaterSourceResponse response = waterSourceService.updateWaterSource(sourceStrId, updateRequest, user.getId());
        return ResponseEntity.ok(new MyApiResponse<>("更新成功", response));
    }

    @DeleteMapping("/{sourceStrId}")
    @ApiOperation(value = "删除水源", notes = "删除一个水源监测点 (及其关联的 Dify 会话)")
    public ResponseEntity<MyApiResponse<String>> deleteWaterSource(
            @ApiParam(value = "水源的字符串ID", example = "source_ulid_mock_abc")
            @PathVariable String sourceStrId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        waterSourceService.deleteWaterSource(sourceStrId, user.getId());
        return ResponseEntity.ok(new MyApiResponse<>("删除成功",null));
    }
}
