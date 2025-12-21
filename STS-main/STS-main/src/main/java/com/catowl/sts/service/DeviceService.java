package com.catowl.sts.service;

import com.catowl.sts.model.dto.Request.DevicePairRequest;
import com.catowl.sts.model.dto.Response.DeviceConfigResponse;
import com.catowl.sts.model.entity.UnclaimedDevice;

import java.util.List;

/**
 * @program: STS
 * @description:
 * @author: qqCatOwlbb
 * @create: 2025-11-23 14:11
 **/
public interface DeviceService {
    void registerDevice(String macAddress);
    DeviceConfigResponse getDeviceConfig(String macAddress);
    List<UnclaimedDevice> getUnclaimedDevices();
    void pairDevice(DevicePairRequest request, Long userId);
    Long validateApiKey(String apiKey);
}
