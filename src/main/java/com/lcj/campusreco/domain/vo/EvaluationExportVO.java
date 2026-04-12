package com.lcj.campusreco.domain.vo;

import lombok.Data;

@Data
public class EvaluationExportVO {

    private String fileName;
    private String filePath;
    private Integer topK;
    private Integer baselineCount;
    private String generatedAt;
}
