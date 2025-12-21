package com.catowl.sts.service.impl;

import com.catowl.sts.exception.BadRequestException;
import com.catowl.sts.exception.InternetServerException;
import com.catowl.sts.mapper.WaterDataMapper;
import com.catowl.sts.mapper.WaterSourceMapper;
import com.catowl.sts.model.dto.Request.DataUploadRequest;
import com.catowl.sts.model.dto.Response.DataQueryResponse;
import com.catowl.sts.model.entity.WaterQualityData;
import com.catowl.sts.model.entity.WaterSource;
import com.catowl.sts.service.WaterDataService;
import com.catowl.sts.utils.RedisCache;
import de.huxhorn.sulky.ulid.ULID;
import io.lettuce.core.RedisException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class WaterDataServiceImpl implements WaterDataService {
    // --- Redis Key 常量 ---
    private static final String RATE_LIMIT_KEY_PREFIX = "ratelimit:upload:source:";
    public static final String CACHE_SOURCE_ID_KEY_PREFIX = "cache:source_id_by_strid:";

    // --- 业务常量 ---
    private static final int RATE_LIMIT_PER_MINUTE = 100;
    public static final long CACHE_SOURCE_ID_TTL_HOURS = 24;

    @Autowired
    private WaterDataMapper waterDataMapper;

    @Autowired
    private WaterSourceMapper waterSourceMapper;

    @Autowired
    private RedisCache redisCache;

    @Override
    @Transactional
    public void uploadData(DataUploadRequest request, Long userId){
        checkRateLimit(request.getSourceStrId());
        Long sourceId = resolveSourceId(request.getSourceStrId(), userId);
        ULID ulidGenerator = new ULID();
        ULID.Value ulid = ulidGenerator.nextValue();

        //构造实体
        /*BigDecimal turData =
                new BigDecimal("865.68")
                        .multiply(BigDecimal.valueOf(5).subtract(request.getTurbidityValue()));*/
        WaterQualityData data = new WaterQualityData();
        data.setStrId(ulid.toString());
        data.setSourceId(sourceId);
        data.setTurbidityValue(request.getTurbidityValue());
        data.setUnit(request.getUnit());
        data.setMeasuredAt(request.getMeasuredAt());
        data.setUploadedAt(LocalDateTime.now());

        waterDataMapper.insertData(data);
    }

    @Override
    public List<DataQueryResponse> queryData(LocalDateTime startTime, LocalDateTime endTime, String sourceStrId, Long userId){
        WaterSource source = waterSourceMapper.findByStrIdAndUserId(sourceStrId, userId);
        if (source == null){
            throw new BadRequestException("水源未找到或无权访问");
        }

        List<WaterQualityData> dataList = waterDataMapper.findDataBySourceIdAndTimeRange(
                source.getId(),
                startTime,
                endTime
        );
        return dataList.stream()
                .map(data -> new DataQueryResponse(
                        data.getStrId(),
                        data.getTurbidityValue(),
                        data.getUnit(),
                        data.getMeasuredAt()
                ))
                .toList();
    }

    private void checkRateLimit(String sourceStrId){
        String key = RATE_LIMIT_KEY_PREFIX + sourceStrId;
        Long count = redisCache.redisTemplate.opsForValue().increment(key);
        if(count != null && count == 1){
            //新key，设置过期时间
            redisCache.expire(key, 60);
        }
        if(count != null && count > RATE_LIMIT_PER_MINUTE){
            throw new BadRequestException("数据上报过于频繁，请稍后再试");
        }
    }

    //解析 sourceId （缓存）
    private Long resolveSourceId(String sourceStrId, Long userId){
        String key = CACHE_SOURCE_ID_KEY_PREFIX + sourceStrId;
        Long sourceId = null;

        Object cachedObject = null;
        try{
            //尝试从redis中获取
            cachedObject = redisCache.getCacheObject(key);
        }catch (RedisException e){
            throw new InternetServerException(e.getMessage());
        }

        if(cachedObject != null){
            //缓存命中
            if(cachedObject instanceof Number){
                sourceId = ((Number) cachedObject).longValue();
            } else{
                throw new InternetServerException("Redis 缓存类型错误");
            }
        }

        WaterSource source = waterSourceMapper.findByStrIdAndUserId(sourceStrId, userId);
        if(source == null){
            throw new BadRequestException("无效水源ID");
        }

        sourceId = source.getId();

        try {
            //因为参数传感器会频繁上报数据，为防止数据库被击穿，通过redis存储sourceId
            redisCache.setCacheObject(key, sourceId, (int) CACHE_SOURCE_ID_TTL_HOURS, TimeUnit.HOURS);
        }catch (RedisException e){
            throw new InternetServerException(e.getMessage());
        }

        return sourceId;
    }
}
