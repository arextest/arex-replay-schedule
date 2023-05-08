package com.arextest.schedule.comparer;

import lombok.Data;

import java.util.List;

/**
 * @author jmo
 * @since 2022/12/5
 */
@Data
public class CategoryComparisonHolder {
    private String categoryName;
    private List<CompareItem> record;
    private List<CompareItem> replayResult;
}