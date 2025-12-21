package com.catowl.sts.mapper;

import com.catowl.sts.model.entity.AnalysisReport;
import com.catowl.sts.model.entity.WaterQualityData;
import com.catowl.sts.model.entity.WaterSource;
import com.catowl.sts.model.vo.ReportDetailVO;
import com.catowl.sts.model.vo.ReportWithSourceDetails;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.security.core.parameters.P;

import java.util.List;

@Mapper
public interface ReportMapper {
    /**
     * 1. 插入新的分析报告
     * (ID 将通过 useGeneratedKeys 回填到 report 对象中)
     */
    int insertReport(AnalysisReport report);

    /**
     * 2. 批量插入报告与数据的关联
     */
    int insertReportDataLink(@Param("reportId") Long reportId, @Param("dataIds")List<Long> dataIds);

    /**
     * 3. 根据 StrId 查找水源信息 (获取 Dify Conversation ID)
     */
    WaterSource findWaterSourceByStrId(@Param("sourceStrId") String sourceStrId);

    /**
     * 4. 根据水源 ID 查询最近的水质数据
     */
    List<WaterQualityData> findRecentDataBySourceId(@Param("sourceId") Long sourceId, @Param("limit") int limit);

    /**
     * 5. 获取用户的所有报告（通过source_id关联user_id）
     */
    List<AnalysisReport> findReportsByUserId(@Param("userId") Long userId, @Param("lastStrId") String lastStrId, @Param("pageSize") int pageSize);

    /**
     * 6. 获取用户拥有的特定报告（私人）
     */
    ReportDetailVO findReportByStrIdAndUserId(@Param("reportStrId") String reportStrId, @Param("userId") Long userId);

    /**
     * 7. 获取已发布的公开报告（公开）
     */
    ReportWithSourceDetails findPublishedReportByStrId(@Param("reportStrId") String reportStrId);

    /**
     * 8. 更新报告发布状态（私人）
     * */
    int updateReportStatusByStrIdAndUserId(@Param("reportStrId") String reportStrId,
                                           @Param("userId") Long userId,
                                           @Param("isPublished") boolean isPublished);

    /**
     * 9. 删除报告（私人）
     * */
    int deleteReportByStrIdAndUserId(@Param("reportStrId") String reportStrId, @Param("userId") Long userId);

    /**
     * 10. 根据水源ID游标分页查询报告标签
     * */
    List<AnalysisReport> findReportTagsBySourceIdWithCursor(
            @Param("sourceStrId") String sourceStrId,
            @Param("userId") Long userId,
            @Param("lastStrId") String lastStrId,
            @Param("pageSize") int pageSize
    );

    /**
     * 根据报告ID查询所有关联的水质数据
     * */
    List<WaterQualityData> findLinkedDataByReportId(@Param("reportId") Long reportId);

}
