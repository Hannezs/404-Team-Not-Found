package com.catowl.sts.controller;

import com.catowl.sts.exception.UnauthorizedException;
import com.catowl.sts.mapper.AuthMapper;
import com.catowl.sts.model.dto.Request.DataUploadRequest;
import com.catowl.sts.model.dto.Request.DevicePairRequest;
import com.catowl.sts.model.dto.Request.DeviceRegisterRequest;
import com.catowl.sts.model.dto.Response.DeviceConfigResponse;
import com.catowl.sts.model.dto.Response.MyApiResponse;
import com.catowl.sts.model.dto.Response.UnclaimedDeviceResponse;
import com.catowl.sts.model.entity.UnclaimedDevice;
import com.catowl.sts.model.entity.User;
import com.catowl.sts.model.entity.WaterSource;
import com.catowl.sts.service.DeviceService;
import com.catowl.sts.service.WaterDataService;
import com.catowl.sts.utils.RedisCache;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Api(tags = "6. 设备配网", description = "处理 ESP32 设备的自动注册与绑定")
@RestController
public class DeviceProvisioningController {
    @Autowired
    private DeviceService deviceService;

    @Autowired
    private WaterDataService waterDataService;

    @PostMapping("/public/api/devices/register")
    @ApiOperation(value = "设备注册 (ESP32)", notes = "设备启动时调用，声明自己在线并等待绑定")
    public ResponseEntity<MyApiResponse<String>> registerDevice(@RequestBody DeviceRegisterRequest deviceRegisterRequest) {
        String macAddress = deviceRegisterRequest.getMacAddress();
        if (macAddress == null || macAddress.isEmpty()) {
            return ResponseEntity.badRequest().body(new MyApiResponse<>("MAC 地址不能为空", null));
        }
        deviceService.registerDevice(macAddress);
        return ResponseEntity.ok(new MyApiResponse<>("设备已注册", null));
    }

    @GetMapping("/public/api/devices/config/{macAddress}")
    @ApiOperation(value = "设备轮询配置 (ESP32)", notes = "设备轮询此接口，检查用户是否已完成绑定")
    public ResponseEntity<DeviceConfigResponse> getDeviceConfig(@PathVariable String macAddress) {
        DeviceConfigResponse config = deviceService.getDeviceConfig(macAddress);
        if (config != null) {
            // 返回 200 和配置信息
            return ResponseEntity.ok(config);
        } else {
            // 返回 404 表示尚未绑定，设备应继续轮询
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/api/devices/unclaimed")
    @ApiOperation(value = "获取待绑定设备列表", notes = "列出所有已注册但未被绑定的设备")
    public ResponseEntity<MyApiResponse<List<UnclaimedDevice>>> getUnclaimedDevices() {
        List<UnclaimedDevice> devices = deviceService.getUnclaimedDevices();
        return ResponseEntity.ok(new MyApiResponse<>("获取成功", devices));
    }

    @PostMapping("/api/devices/pair")
    @ApiOperation(value = "绑定设备", notes = "将选定的设备与当前用户的水源绑定")
    public ResponseEntity<MyApiResponse<String>> pairDevice(@Valid @RequestBody DevicePairRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();

        deviceService.pairDevice(request, user.getId());
        return ResponseEntity.ok(new MyApiResponse<>("设备绑定成功", null));
    }

    @PostMapping("/api/devices/upload")
    @ApiOperation(value = "设备上传数据 (API Key)", notes = "供已绑定的设备上传数据")
    public ResponseEntity<MyApiResponse<String>> uploadDataFromDevice(
            @Valid @RequestBody DataUploadRequest dataRequest) { // 移除了 HttpServletRequest 参数

        // 1. 直接从 SecurityContext 获取用户 (Filter 已经放进去了)
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 双重保险：虽然 Filter 验证过了，但如果 Filter 放行了(比如没带Header)这里会是 null
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            throw new UnauthorizedException("设备未认证");
        }

        User user = (User) authentication.getPrincipal();

        // 2. 调用业务逻辑
        waterDataService.uploadData(dataRequest, user.getId());

        return new ResponseEntity<>(new MyApiResponse<>("数据上传成功", null), HttpStatus.CREATED);
    }
}
