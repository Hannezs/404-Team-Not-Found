package com.catowl.sts.model.vo;

import com.catowl.sts.model.entity.AnalysisReport;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportWithSourceDetails extends AnalysisReport {
    private String sourceName;
    private String sourceType;
}
