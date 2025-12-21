package com.catowl.sts.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportDataLink implements Serializable {
    private Long reportId;
    private Long dataId;
}
