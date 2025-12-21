package com.catowl.sts.mapper;

import com.catowl.sts.model.dto.Response.DeviceConfigResponse;
import com.catowl.sts.model.entity.UnclaimedDevice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @program: STS
 * @description:
 * @author: qqCatOwlbb
 * @create: 2025-11-23 14:07
 **/
@Mapper
public interface DeviceMapper {
    // 插入或更新未认领设备
    int upsertUnclaimedDevice(@Param("macAddress") String macAddress);

    // 获取所有未认领设备
    List<UnclaimedDevice> findAllUnclaimedDevices();

    // 删除未认领设备
    int deleteUnclaimedDevice(@Param("macAddress") String macAddress);

    // 创建绑定关系
    int insertDevicePairing(@Param("macAddress") String macAddress, @Param("sourceId") Long sourceId);

    // 检查设备是否已绑定
    int countPairingByMac(@Param("macAddress") String macAddress);

    // 获取设备的配置信息 (关联查询 users 和 water_sources)
    DeviceConfigResponse findConfigByMacAddress(@Param("macAddress") String macAddress);

    // 根据 API Key 查找用户 ID (用于数据上传鉴权)
    Long findUserIdByApiKey(@Param("apiKey") String apiKey);
}
