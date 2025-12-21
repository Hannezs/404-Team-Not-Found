package com.catowl.sts.mapper;

import com.catowl.sts.model.entity.WaterSource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.security.core.parameters.P;

import java.util.List;

@Mapper
public interface WaterSourceMapper {
    /**
     * 新增水源
     * */
    int insertSource(WaterSource source);

    /**
     * 根据 StrId 和 UserId 删除水源
     * */
    int deleteSourceByStrIdAndUserId(@Param("strId") String strId, @Param("userId") Long userId);

    /**
     * 更新水源信息
     * */
    int updateSource(WaterSource source);

    /**
     * （公开）根据 StrId 查找水源（缓存回馈）
     * */
    WaterSource findByStrIdAndUserId(@Param("strId") String strId, @Param("userId") Long userId);

    /**
     * （私有）查询用户的所有水源
     * */
    List<WaterSource> findSourcesByUserIdWithCursor(
            @Param("userId") Long userId,
            @Param("lastStrId") String lastStrId,
            @Param("pageSize") int pageSize,
            @Param("searchName") String searchName
    );
}
