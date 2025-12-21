package com.catowl.sts.service;

import com.catowl.sts.model.dto.Response.ReportPublicResponse;

public interface PublicReportService {

    /**
     * 获取已公开的报告，带缓存
     * */
    ReportPublicResponse getPublicReport(String reportStrId);
}
