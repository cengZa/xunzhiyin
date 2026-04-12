package com.lcj.campusreco.common.api;

import java.util.Collections;
import java.util.List;
import lombok.Data;

@Data
public class PageResponse<T> {

    private long pageNo;
    private long pageSize;
    private long total;
    private List<T> records = Collections.emptyList();
}
