package com.catowl.sts.service.impl;

import com.catowl.sts.exception.BadRequestException;
import com.catowl.sts.mapper.AuthMapper;
import com.catowl.sts.mapper.DeviceMapper;
import com.catowl.sts.mapper.WaterSourceMapper;
import com.catowl.sts.model.dto.Request.DevicePairRequest;
import com.catowl.sts.model.dto.Response.DeviceConfigResponse;
import com.catowl.sts.model.entity.UnclaimedDevice;
import com.catowl.sts.model.entity.User;
import com.catowl.sts.model.entity.WaterSource;
import com.catowl.sts.service.DeviceService;
import com.catowl.sts.utils.RedisCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @program: STS
 * @description: crud，偷偷懒
 * @author: qqCatOwlbb
 * @create: 2025-11-23 14:12
 **/
@Service
public class DeviceServiceImpl implements DeviceService {
    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    private WaterSourceMapper waterSourceMapper;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private AuthMapper userMapper;

    @Override
    public void registerDevice(String macAddress) {
        // 如果设备已经绑定过，就不加入未认领列表
        if (deviceMapper.countPairingByMac(macAddress) > 0) {
            return;
        }
        deviceMapper.upsertUnclaimedDevice(macAddress);
    }

    @Override
    public DeviceConfigResponse getDeviceConfig(String macAddress) {
        return deviceMapper.findConfigByMacAddress(macAddress);
    }

    @Override
    public List<UnclaimedDevice> getUnclaimedDevices() {
        return deviceMapper.findAllUnclaimedDevices();
    }

    @Override
    @Transactional
    public void pairDevice(DevicePairRequest request, Long userId) {
        // 1. 验证水源是否属于当前用户
        WaterSource source = waterSourceMapper.findByStrIdAndUserId(request.getSourceStrId(), userId);
        if (source == null) {
            throw new BadRequestException("无效的水源或无权访问");
        }

        // 2. 检查设备是否已被绑定
        if (deviceMapper.countPairingByMac(request.getMacAddress()) > 0) {
            throw new BadRequestException("该设备已被绑定");
        }

        // 3. 创建绑定关系
        deviceMapper.insertDevicePairing(request.getMacAddress(), source.getId());

        // 4. 从未认领列表中删除
        deviceMapper.deleteUnclaimedDevice(request.getMacAddress());

        User user = userMapper.selectUser(userId);
        user.setId(userId);
        String redisKey = "device:auth:" + user.getApiKey();
        redisCache.setCacheObject(redisKey, user, 24, TimeUnit.HOURS);
    }

    @Override
    public Long validateApiKey(String apiKey) {
        return deviceMapper.findUserIdByApiKey(apiKey);
    }
}
