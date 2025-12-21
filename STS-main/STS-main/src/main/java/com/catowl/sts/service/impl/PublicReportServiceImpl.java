package com.catowl.sts.service.impl;

import com.catowl.sts.exception.BadRequestException;
import com.catowl.sts.exception.InternetServerException;
import com.catowl.sts.mapper.ReportMapper;
import com.catowl.sts.model.dto.Response.ReportPublicResponse;
import com.catowl.sts.model.vo.ReportWithSourceDetails;
import com.catowl.sts.service.PublicReportService;
import com.catowl.sts.utils.RedisCache;
import io.lettuce.core.RedisException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class PublicReportServiceImpl implements PublicReportService {
    @Autowired
    private ReportMapper reportMapper;

    @Autowired
    private RedisCache redisCache;

    @Override
    public ReportPublicResponse getPublicReport(String reportStrId){
        String cacheKey = ReportServiceImpl.PUBLIC_REPORT_KEY_PREFIX + reportStrId;
        try {
            ReportPublicResponse cachedResponse = redisCache.getCacheObject(cacheKey);
            if(cachedResponse != null){
                return cachedResponse;
            }
        } catch (RedisException e){
            throw new InternetServerException(e.getMessage());
        }

        //缓存未命中
        ReportWithSourceDetails report = reportMapper.findPublishedReportByStrId(reportStrId);
        if(report == null){
            throw new BadRequestException("报告未找到或未发布");
        }

        ReportPublicResponse responseDto = convertToPublicResponseDto(report);
        try {
            redisCache.setCacheObject(
                    cacheKey,
                    responseDto,
                    (int) ReportServiceImpl.PUBLIC_REPORT_CACHE_HOURS,
                    TimeUnit.HOURS
            );
        }catch (RedisException e){
            throw new InternetServerException(e.getMessage());
        }

        return responseDto;
    }

    private ReportPublicResponse convertToPublicResponseDto(ReportWithSourceDetails report){
        ReportPublicResponse dto = new ReportPublicResponse();
        dto.setStrId(report.getStrId());
        dto.setSourceType(report.getSourceType());
        dto.setSourceName(report.getSourceName());
        dto.setReportContent(report.getReportContent());
        dto.setQrCodePath(report.getQrCodePath());
        dto.setGeneratedAt(report.getGeneratedAt());
        dto.setKeywords(report.getKeywords());
        return dto;
    }
}
