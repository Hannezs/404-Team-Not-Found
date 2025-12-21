package com.catowl.sts.service;

import com.catowl.sts.model.dto.Request.DataUploadRequest;
import com.catowl.sts.model.dto.Response.DataQueryResponse;

import java.time.LocalDateTime;
import java.util.List;

public interface WaterDataService {

    void uploadData(DataUploadRequest request, Long userId);

    List<DataQueryResponse> queryData(LocalDateTime startTime, LocalDateTime endTime, String sourceStrId, Long userId);
}
