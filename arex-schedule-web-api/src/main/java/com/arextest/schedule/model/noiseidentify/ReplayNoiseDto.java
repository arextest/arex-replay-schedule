package com.arextest.schedule.model.noiseidentify;

import java.util.List;
import java.util.Map;

import com.arextest.diff.model.log.NodeEntity;
import com.arextest.schedule.model.ReplayCompareResult;

import lombok.Data;

@Data
public class ReplayNoiseDto {
    private String planId;
    private String planItemId;

    private String categoryName;

    private String operationName;

    // k: path v: data
    private Map<String, ReplayNoiseItem> mayIgnoreItems;

    private Map<String, ReplayNoiseItem> mayDisorderItems;

    @Data
    public static class ReplayNoiseItem {

        private List<NodeEntity> nodePath;

//        private String compareResultId;

        private transient ReplayCompareResult compareResult;

        private List<Integer> logIndexes;

        private Map<String, Integer> subPaths;

        private Integer pathCount;

        private Integer caseCount;
    }

}
