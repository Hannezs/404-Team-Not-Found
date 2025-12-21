package com.catowl.sts.service;

import com.catowl.sts.model.dto.Request.WaterSourceCreateRequest;
import com.catowl.sts.model.dto.Request.WaterSourceUpdateRequest;
import com.catowl.sts.model.dto.Response.WaterSourceResponse;
import com.catowl.sts.model.entity.User;

import java.util.List;

public interface WaterSourceService {

    WaterSourceResponse createWaterSource(WaterSourceCreateRequest request, User user);

    void deleteWaterSource(String sourceStrId, Long userId);

    WaterSourceResponse updateWaterSource(String sourceStrId, WaterSourceUpdateRequest request, Long userId);

    WaterSourceResponse getWaterSource(String sourceStrId,Long userId);

    List<WaterSourceResponse> getAllWaterSources(Long userId, String lastStrId, int pageSize, String searchName);
}
