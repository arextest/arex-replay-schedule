package com.arextest.replay.schedule.model.config;

import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by wang_yc on 2021/10/14
 */
@Data
public class ReplayComparisonConfig {
    // ignore according to type
    private List<String> ignoreTypeList;
    // ignore according to interface
    private List<String> ignoreKeyList;

    private Set<List<String>> exclusionList;
    private Set<List<String>> inclusionList;

    private Map<List<String>, List<String>> referenceMap;
    private Map<List<String>, List<List<String>>> listSortMap;
    private Map<String, List<List<String>>> decompressConfig;

    public final boolean checkIgnoreMockMessageType(String type) {
        return ignoreTypeList != null && ignoreTypeList.contains(type);
    }
}
