package com.catowl.sts.service;

import com.catowl.sts.model.dto.Request.ReportGenerateRequest;
import com.catowl.sts.model.dto.Response.ReportDetailResponse;
import com.catowl.sts.model.dto.Response.ReportResponse;
import com.catowl.sts.model.dto.Response.ReportStatusUpdateResponse;
import com.catowl.sts.model.dto.Response.ReportTagResponse;

import java.util.List;

public interface ReportService {

    ReportResponse generateReport(ReportGenerateRequest request);

    List<ReportTagResponse> getReportsForUser(Long userId, String lastStrId, int pageSize);

    ReportDetailResponse getReportByIdForUser(String reportStrId, Long userId);

    ReportStatusUpdateResponse publishReport(String reportStrId, Long userId);

    ReportStatusUpdateResponse unpublishReport(String reportStrId, Long userId);

    void deleteReport(String reportStrId,Long userId);

    List<ReportTagResponse> getReportTagsBySourceId(String sourceStrId,Long userId,String lastStrId,int pageSize);
}
