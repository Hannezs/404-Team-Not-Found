package com.catowl.sts.model.entity;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(
        value = "水源信息实体类",
        description = "水源的基本信息",
        parent = Object.class
)
public class WaterSource implements Serializable {

    private Long id;

    private String strId;

    private String sourceName;

    private String sourceType;

    private String difyConversationId;

    private String description;

    private Boolean isActive;

    private LocalDateTime createdAt;

    private Long userId;
}
