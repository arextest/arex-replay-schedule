package com.arextest.schedule.spi.model;

import lombok.Data;

import java.util.Map;

@Data
public class ReplayActionCaseItem {
    private String id;
    private String planItemId;
    private String recordId;
    private String targetResultId;
    private String sourceResultId;
    private String contextIdentifier;
    private String caseType;
    private Target targetRequest;
    //private ReplayActionItem parent;

    @Data
    public static class ReplayActionItem {

    }

    @Data
    public static class Target {
        private String body;
        private Map<String, Object> attributes;
        private String type;
    }
}
