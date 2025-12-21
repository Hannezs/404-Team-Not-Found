package com.catowl.sts.service.impl;

import com.catowl.sts.exception.BadRequestException;
import com.catowl.sts.exception.InternetServerException;
import com.catowl.sts.mapper.ReportMapper;
import com.catowl.sts.model.dto.Request.ReportGenerateRequest;
import com.catowl.sts.model.dto.Response.*;
import com.catowl.sts.model.entity.AnalysisReport;
import com.catowl.sts.model.entity.User;
import com.catowl.sts.model.entity.WaterQualityData;
import com.catowl.sts.model.entity.WaterSource;
import com.catowl.sts.model.vo.ReportDetailVO;
import com.catowl.sts.model.vo.ReportWithSourceDetails;
import com.catowl.sts.service.ReportService;
import com.catowl.sts.utils.MyQrCodeUtil;
import com.catowl.sts.utils.RedisCache;
import de.huxhorn.sulky.ulid.ULID;
import io.github.imfangs.dify.client.DifyChatflowClient;
import io.github.imfangs.dify.client.enums.ResponseMode;
import io.github.imfangs.dify.client.exception.DifyApiException;
import io.github.imfangs.dify.client.model.chat.ChatMessage;
import io.github.imfangs.dify.client.model.chat.ChatMessageResponse;
import org.ansj.domain.Term;
import org.ansj.splitWord.analysis.NlpAnalysis;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {
    // --- Redis Key 常量 ---
    private static final String RATE_LIMIT_KEY_PREFIX = "ratelimit:report:user:";
    private static final String LOCK_KEY_PREFIX = "lock:report:source:";
    public static final String PUBLIC_REPORT_KEY_PREFIX = "cache:report:public:";

    // --- 业务常量 ---
    private static final int MAX_REPORT_ATTEMPTS = 50; // 10分钟内最大尝试次数
    private static final long RATE_LIMIT_WINDOW_MIN = 10; // 10分钟窗口
    private static final long LOCK_TIMEOUT_SEC = 60; // 锁超时时间60秒，防止死锁
    private static final int RECENT_DATA_LIMIT = 40; // 查询最近20条数据
    public static final long PUBLIC_REPORT_CACHE_HOURS = 24;

    @Autowired
    private ReportMapper reportMapper;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private DifyChatflowClient chatflowClient;

    @Value("${file.qrcode-dir}")
    private String qrDir;

    @Value("${frontend.base-url}")
    private String frontendBaseUrl;

    @Override
    @Transactional
    public ReportResponse generateReport(ReportGenerateRequest request){
        // 限流
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        checkRateLimit(user.getId());

        String sourceStrId = request.getSourceStrId();
        String lockKey = LOCK_KEY_PREFIX + sourceStrId;

        // 分布式锁
        Boolean acquired = redisCache.redisTemplate.opsForValue().setIfAbsent(lockKey,user.getId().toString(),LOCK_TIMEOUT_SEC, TimeUnit.SECONDS);

        if(acquired == null || !acquired) {
            throw new BadRequestException("报告正在生成中，请勿重复提交");
        }

        // 获取业务数据
        WaterSource waterSource = reportMapper.findWaterSourceByStrId(sourceStrId);
        if(waterSource == null || !acquired) {
            throw new BadRequestException("该用户不存在该水源");
        }
        List<WaterQualityData> dataList = reportMapper.findRecentDataBySourceId(waterSource.getId(),RECENT_DATA_LIMIT);
        if(dataList == null || dataList.isEmpty()) {
            throw new BadRequestException("该水源近期无水质数据，无法生成报告");
        }

        String formattedData = formatDataForDify(dataList);
        ChatMessage message = buildDifyRequest(waterSource,formattedData,user);
        try {
            ChatMessageResponse response = chatflowClient.sendChatMessage(message);
            String reportContent = response.getAnswer();
            String difyMessageId = response.getMessageId();
            AnalysisReport report = saveReportToDb(reportContent,difyMessageId,waterSource.getId(),dataList);
            report.setSourceStrId(sourceStrId);
            return convertToResponseDTO(report);
        } catch (IOException | DifyApiException e) {
            throw new InternetServerException("服务器异常，请将错误信息提交给管理员" + e.getMessage());
        } finally {
            redisCache.deleteObject(lockKey);
        }
    }

    //限流函数
    private void checkRateLimit(Long userId){
        String rateLimitKey = RATE_LIMIT_KEY_PREFIX + userId;
        Long currentCount = redisCache.redisTemplate.opsForValue().increment(rateLimitKey);
        if(currentCount == null){
            throw new InternetServerException("redis服务异常");
        }
        if(currentCount == 1){
            redisCache.expire(rateLimitKey,RATE_LIMIT_WINDOW_MIN,TimeUnit.MINUTES);
        }
        if(currentCount > MAX_REPORT_ATTEMPTS) {
            throw new BadRequestException("操作过于频繁，请10分钟后再试");
        }
    }

    //格式化数据库数据提交给dify
    private String formatDataForDify(List<WaterQualityData> dataList){
        //返回格式："150.1, 12.2, 812"
        return dataList.stream()
                .map(data->data.getTurbidityValue().toString())
                .collect(Collectors.joining(", "));
    }

    private ChatMessage buildDifyRequest(WaterSource waterSource,String formattedData, User user){
        return ChatMessage.builder()
                .query("请根据最新数据生成分析报告")
                .user(user.getStrId())
                .inputs(Map.of(
                        "sourceName", waterSource.getSourceName(),
                        "sourceType", waterSource.getSourceType(),
                        "description", waterSource.getDescription(),
                        "recent_turbidity_data", formattedData))
                .conversationId(waterSource.getDifyConversationId())
                .responseMode(ResponseMode.BLOCKING)
                .build();
    }

    //保存报告到数据库并生成二维码
    @Transactional
    public AnalysisReport saveReportToDb(String reportContent, String difyMessageId, Long sourceId, List<WaterQualityData> dataList){
        AnalysisReport report = new AnalysisReport();
        ULID ulidGenerator = new ULID();
        ULID.Value ulid = ulidGenerator.nextValue();
        report.setStrId(ulid.toString());
        report.setSourceId(sourceId);
        report.setReportContent(reportContent);
        report.setDifyMessageId(difyMessageId);
        report.setPublished(false);
        report.setGeneratedAt(LocalDateTime.now());

        //关键词提取
        List<Term> terms = NlpAnalysis.parse(reportContent).getTerms();
        List<String> nouns = terms.stream()
                .filter(term -> term.getNatureStr() != null && term.getNatureStr().startsWith("n"))
                .map(Term::getName)
                .filter(name -> name.length() > 1)
                .distinct()
                .toList();
        List<String> focusWords = Arrays.asList(
                "清澈", "污染", "混浊", "良好", "恶化", "改善", "下降", "升高", "波动",

                // 二、可能原因相关
                "降雨", "天气", "气候", "温度", "泥沙", "底泥", "施工", "人工活动", "排水", "藻类", "水生植物",
                "扰动", "沉降", "流动", "外来物", "杂质",

                // 三、措施与建议相关
                "治理", "调控", "过滤", "净化", "修复", "管理", "监测", "保持", "维护", "生态",
                "沉淀", "清理", "防护", "控制", "优化", "水循环", "稳定"
        );
        String keywords = nouns.stream()
                .filter(word -> focusWords.stream().anyMatch(word::contains))
                .limit(5)
                .collect(Collectors.joining(","));
        report.setKeywords(keywords);

        //生成二维码
        /*File reportDir = new File(qrDir + File.separator + report.getStrId());
        String fileName = difyMessageId + ".png";
        String contentUrl = frontendBaseUrl + "/reports/" + report.getStrId();
        MyQrCodeUtil.createCodeToFile("https://www.bilibili.com/video/BV1GJ411x7h7/?spm_id_from=333.337.search-card.all.click&vd_source=b9e088f55d435bcece3c1df8435e38ee", reportDir,fileName);

        String urlPath = "/qrcode/" + report.getStrId() + "/" +fileName;*/
        report.setQrCodePath("nothing");
        reportMapper.insertReport(report);
        List<Long> dataIds = dataList.stream().map(WaterQualityData::getId).collect(Collectors.toList());
        reportMapper.insertReportDataLink(report.getId(),dataIds);
        return report;
    }

    //把saveReportToDb返回的报告包装成DTO
    private ReportResponse convertToResponseDTO(AnalysisReport report){
        ReportResponse dto = new ReportResponse();
        dto.setStrId(report.getStrId());
        dto.setSourceStrId(report.getSourceStrId());
        dto.setReportContent(report.getReportContent());
        dto.setQrCodePath(report.getQrCodePath());
        dto.setGeneratedAt(report.getGeneratedAt());
        dto.setIsPublished(report.isPublished());
        dto.setKeywords(report.getKeywords());
        return dto;
    }

    @Override
    public List<ReportTagResponse> getReportsForUser(Long userId, String lastStrId, int pageSize){
        List<AnalysisReport> reports = reportMapper.findReportsByUserId(userId, lastStrId, pageSize);
        return reports.stream()
                .map(report -> new ReportTagResponse(report.getStrId(), report.getKeywords(), report.getGeneratedAt()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ReportDetailResponse getReportByIdForUser(String reportStrId, Long userId){
        ReportDetailVO report = reportMapper.findReportByStrIdAndUserId(reportStrId, userId);
        if(report == null){
            throw new BadRequestException("报告未找到或无权访问");
        }

        List<WaterQualityData> dataList = reportMapper.findLinkedDataByReportId(report.getId());
        List<DataQueryResponse> dataResponseList = dataList.stream()
                .map(data -> new DataQueryResponse(
                        data.getStrId(),
                        data.getTurbidityValue(),
                        data.getUnit(),
                        data.getMeasuredAt()
                ))
                .toList();
        ReportDetailResponse response = new ReportDetailResponse();
        response.setFromVO(report);
        response.setUsedData(dataResponseList);
        return response;
    }

    @Override
    public List<ReportTagResponse> getReportTagsBySourceId(String sourceStrId, Long userId, String lastStrId, int pageSize){
        return reportMapper.findReportTagsBySourceIdWithCursor(
                sourceStrId,
                userId,
                lastStrId,
                pageSize
        ).stream()
                .map(report -> new ReportTagResponse(report.getStrId(),report.getKeywords(),report.getGeneratedAt()))
                .toList();
    }

    @Override
    @Transactional
    public ReportStatusUpdateResponse publishReport(String reportStrId, Long userId){
        int rowsAffected = reportMapper.updateReportStatusByStrIdAndUserId(reportStrId, userId, true);
        if (rowsAffected == 0){
           throw new BadRequestException("报告未找到或无权访问");
        }

        // 预热/刷新 redis 缓存，因为公开的报告可能有大量访问，我们需要将其存入redis，虽然我觉得老师多半不管你有没用这些中间件就是了0.0
        // 从数据库查询已发布的报告
        ReportWithSourceDetails publishedReport = reportMapper.findPublishedReportByStrId(reportStrId);
        if (publishedReport != null){
            ReportPublicResponse publicDto = convertToPublicResponseDto(publishedReport);

            redisCache.setCacheObject(
                    PUBLIC_REPORT_KEY_PREFIX + reportStrId,
                    publicDto,
                    (int) PUBLIC_REPORT_CACHE_HOURS,
                    TimeUnit.HOURS
            );
        }

        return new ReportStatusUpdateResponse(reportStrId, true);
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

    @Override
    @Transactional
    public ReportStatusUpdateResponse unpublishReport(String reportStrId,Long userId){
        int rowsAffected = reportMapper.updateReportStatusByStrIdAndUserId(reportStrId, userId, false);
        if(rowsAffected == 0){
            throw new BadRequestException("报告未找到或无权访问");
        }

        redisCache.deleteObject(PUBLIC_REPORT_KEY_PREFIX + reportStrId);

        return new ReportStatusUpdateResponse(reportStrId,false);
    }

    @Override
    @Transactional
    public void deleteReport(String reportStrId, Long userId){
        int rowsAffected = reportMapper.deleteReportByStrIdAndUserId(reportStrId, userId);
        if(rowsAffected == 0){
            throw new BadRequestException("报告未找到或无权访问");
        }
        //如果存在会删除，不存在则无事发生
        redisCache.deleteObject(PUBLIC_REPORT_KEY_PREFIX + reportStrId);
    }

    private void deleteOldQr(String oldQrUrl) {
        if (oldQrUrl != null &&oldQrUrl.startsWith("/qrcode/")){
            String oldFileName=oldQrUrl.substring("/qrcode/".length());
            Path oldFilePath= Paths.get(qrDir,oldFileName);
            try{
                Files.deleteIfExists(oldFilePath);
            } catch (IOException e) {
                throw new InternetServerException("删除二维码失败");
            }
        }
    }
}
