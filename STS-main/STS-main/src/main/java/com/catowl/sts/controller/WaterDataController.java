package com.catowl.sts.controller;

import com.catowl.sts.model.dto.Request.DataUploadRequest;
import com.catowl.sts.model.dto.Response.MyApiResponse;
import com.catowl.sts.model.dto.Response.DataQueryResponse;
import com.catowl.sts.model.dto.Response.DataUploadResponse;
import com.catowl.sts.model.entity.User;
import com.catowl.sts.service.WaterDataService;
import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/water-data")
@Api(tags = "3. 水质数据", description = "处理传感器数据的上传和查询")
public class WaterDataController {

    @Autowired
    private WaterDataService waterDataService;

    @PostMapping("/upload")
    @ApiOperation(value = "上传水质数据", notes = "供传感器或边缘设备调用，上传单条浊度数据")
    @ApiResponses({
            @ApiResponse(code = 202, message = "数据已接收")
    })
    public ResponseEntity<MyApiResponse<String>> uploadData(
            @Valid @RequestBody DataUploadRequest dataRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        waterDataService.uploadData(dataRequest,user.getId());
        return new ResponseEntity<>(new MyApiResponse<>("数据上传成功", null),HttpStatus.ACCEPTED);
    }

    @GetMapping("/query/{sourceStrId}")
    @ApiOperation(value = "查询水质历史数据", notes = "根据水源ID查询近期的水质数据")
    public ResponseEntity<MyApiResponse<List<DataQueryResponse>>> queryData(
            @ApiParam(value = "水源的字符串ID", example = "source_ulid_mock_abc")
            @PathVariable String sourceStrId,
            @ApiParam(value = "查询开始时间 (ISO 8601)") @RequestParam(required = false) LocalDateTime startTime,
            @ApiParam(value = "查询结束时间 (ISO 8601)") @RequestParam(required = false) LocalDateTime endTime) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        List<DataQueryResponse> dataList = waterDataService.queryData(startTime,endTime,sourceStrId, user.getId());
        return ResponseEntity.ok(new MyApiResponse<>("获取近期数据成功",dataList));
    }
}
