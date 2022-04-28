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
    private List<String> ignoreTypeList;
    private List<String> ignoreKeyList;
    private Set<String> ignoreNodeList;
    private List<String> ignorePathList;
    private List<String> inclusionList;
    private Map<String, String> referenceList;
    private Map<String, String> listKeyList;
    private Map<String, List<String>> decompressConfig;

    public final boolean checkIgnoreMockMessageType(String type) {
        return ignoreTypeList != null && ignoreTypeList.contains(type);
    }
}
