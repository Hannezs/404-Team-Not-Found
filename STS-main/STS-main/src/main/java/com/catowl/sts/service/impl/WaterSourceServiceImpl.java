package com.catowl.sts.service.impl;

import com.catowl.sts.exception.BadRequestException;
import com.catowl.sts.exception.InternetServerException;
import com.catowl.sts.mapper.WaterSourceMapper;
import com.catowl.sts.model.dto.Request.WaterSourceCreateRequest;
import com.catowl.sts.model.dto.Request.WaterSourceUpdateRequest;
import com.catowl.sts.model.dto.Response.WaterSourceResponse;
import com.catowl.sts.model.entity.User;
import com.catowl.sts.model.entity.WaterSource;
import com.catowl.sts.service.WaterSourceService;
import com.catowl.sts.utils.RedisCache;
import de.huxhorn.sulky.ulid.ULID;
import io.github.imfangs.dify.client.DifyChatflowClient;
import io.github.imfangs.dify.client.enums.ResponseMode;
import io.github.imfangs.dify.client.exception.DifyApiException;
import io.github.imfangs.dify.client.model.chat.ChatMessage;
import io.github.imfangs.dify.client.model.chat.ChatMessageResponse;
import io.lettuce.core.RedisException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class WaterSourceServiceImpl implements WaterSourceService {

    // 水源详情缓存 Key
    public static final String CACHE_SOURCE_DETAILS_KEY_PREFIX = "cache:source_details_by_strid:";
    // 缓存有效期 (例如 1 小时)
    private static final long CACHE_SOURCE_DETAILS_TTL_HOURS = 1;

    @Autowired
    private WaterSourceMapper waterSourceMapper;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private DifyChatflowClient chatflowClient;

    @Override
    @Transactional
    public WaterSourceResponse createWaterSource(WaterSourceCreateRequest request, User user){
        ChatMessage message = buildDifyRequest(request,user);
        try {
            ChatMessageResponse response = chatflowClient.sendChatMessage(message);
            String conversationId = response.getConversationId();
            if(conversationId == null){
                throw new InternetServerException("Dify 服务调用失败");
            }
            ULID ulidGenerator = new ULID();
            ULID.Value ulid = ulidGenerator.nextValue();

            WaterSource source = new WaterSource();
            source.setStrId(ulid.toString());
            source.setUserId(user.getId());
            source.setSourceName(request.getSourceName());
            source.setSourceType("环境水源");
            source.setDescription(request.getDescription());
            source.setDifyConversationId(conversationId);
            source.setCreatedAt(LocalDateTime.now());
            source.setIsActive(true);
            waterSourceMapper.insertSource(source);

            String key = WaterDataServiceImpl.CACHE_SOURCE_ID_KEY_PREFIX + source.getStrId();
            redisCache.setCacheObject(
                    key,
                    source.getId(),
                    (int) WaterDataServiceImpl.CACHE_SOURCE_ID_TTL_HOURS,
                    TimeUnit.HOURS

            );

            return convertToResponseDto(source);
        } catch (IOException | DifyApiException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @Transactional
    public void deleteWaterSource(String sourceStrId, Long userId){
        int rowAffected = waterSourceMapper.deleteSourceByStrIdAndUserId(sourceStrId,userId);
        if(rowAffected == 0){
            throw new BadRequestException("水源未找到或无权删除");
        }
        String key = WaterDataServiceImpl.CACHE_SOURCE_ID_KEY_PREFIX + sourceStrId;
        redisCache.deleteObject(key);

        String detailsCacheKey = CACHE_SOURCE_DETAILS_KEY_PREFIX + sourceStrId + userId.toString();
        redisCache.deleteObject(detailsCacheKey);
    }

    @Override
    @Transactional
    public WaterSourceResponse updateWaterSource(String sourceStrId, WaterSourceUpdateRequest request, Long userId){
        WaterSource source = waterSourceMapper.findByStrIdAndUserId(sourceStrId, userId);
        if(source == null){
            throw new BadRequestException("水源未找到或无权更新");
        }
        boolean updated = false;
        if (request.getSourceName() != null && !request.getSourceName().equals(source.getSourceName())){
            source.setSourceName(request.getSourceName());
            updated = true;
        }
        if (request.getDescription() != null && !request.getDescription().equals(source.getDescription())){
            source.setDescription(request.getDescription());
            updated = true;
        }
        if (request.getIsActive() != null && !request.getIsActive().equals(source.getIsActive())){
            source.setIsActive(request.getIsActive());
            updated = true;
        }

        if(updated){
            waterSourceMapper.updateSource(source);
            String detailsCacheKy = CACHE_SOURCE_DETAILS_KEY_PREFIX + sourceStrId + userId.toString();
            redisCache.deleteObject(detailsCacheKy);
        }

        return convertToResponseDto(source);
    }

    @Override
    public WaterSourceResponse getWaterSource(String sourceStrId, Long userId){
        String cacheKey = CACHE_SOURCE_DETAILS_KEY_PREFIX + sourceStrId + userId.toString();

        WaterSourceResponse cachedResponse = null;
        try{
            cachedResponse = redisCache.getCacheObject(cacheKey);
        }catch (RedisException e){
            throw new InternetServerException(e.getMessage());
        }
        if(cachedResponse == null){
            //缓存未命中，尝试查表
            WaterSource source = waterSourceMapper.findByStrIdAndUserId(sourceStrId, userId);
            if(source == null){
                throw new BadRequestException("水源未找到或无权访问");
            }
            WaterSourceResponse responseDto = convertToResponseDto(source);
            try {
                redisCache.setCacheObject(
                        cacheKey,
                        responseDto,
                        (int) CACHE_SOURCE_DETAILS_TTL_HOURS,
                        TimeUnit.HOURS
                );
            }catch (RedisException e){
                throw new InternetServerException(e.getMessage());
            }
            return responseDto;
        }
        return cachedResponse;
    }

    @Override
    public List<WaterSourceResponse> getAllWaterSources(Long userId, String lastStrId, int pageSize, String searchName){
        List<WaterSource> sources = waterSourceMapper.findSourcesByUserIdWithCursor(userId, lastStrId, pageSize, searchName);
        return sources.stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    private ChatMessage buildDifyRequest(WaterSourceCreateRequest request, User user){
        return ChatMessage.builder()
                .query("null")
                .user(user.getStrId())
                .inputs(Map.of("sourceName", request.getSourceName(),
                        "sourceType",request.getSourceType(),
                "description", request.getDescription()))
                .responseMode(ResponseMode.BLOCKING)
                .build();
    }

    private WaterSourceResponse convertToResponseDto(WaterSource source){
        WaterSourceResponse dto = new WaterSourceResponse();
        BeanUtils.copyProperties(source,dto);
        return dto;
    }
}
