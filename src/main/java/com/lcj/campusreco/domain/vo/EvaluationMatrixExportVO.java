package com.lcj.campusreco.domain.vo;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class EvaluationMatrixExportVO {

    private String fileName;
    private String filePath;
    private Integer experimentCount;
    private List<Integer> topKValues = new ArrayList<>();
}
