package com.catowl.sts.mapper;

import com.catowl.sts.model.entity.WaterQualityData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Mapper
public interface WaterDataMapper {
    /**
     * 批量插入水质数据
     * */
    int insertDataBatch(@Param("dataList")List<WaterQualityData> dataList);

    /**
     * 插入单条水质数据
     * */
    int insertData(WaterQualityData data);

    /**
     * 更具水源ID和时间范围查找数据
     * */
    List<WaterQualityData> findDataBySourceIdAndTimeRange(
            @Param("sourceId") Long sourceId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime")LocalDateTime endTime
            );
}
