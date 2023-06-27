package com.arextest.schedule.service;

import com.arextest.schedule.client.HttpWepServiceApiClient;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

@Service
public final class ConfigurationService {
    @Resource
    private HttpWepServiceApiClient wepApiClientService;
    @Value("${arex.report.config.application.url}")
    private String applicationUrl;
    @Value("${arex.report.config.schedule.url}")
    private String scheduleUrl;

    public Application application(String appId) {
        ApplicationResponse applicationResponse = wepApiClientService.get(applicationUrl, appIdUrlVariable(appId),
                ApplicationResponse.class);
        return applicationResponse != null ? applicationResponse.body : null;
    }

    public ScheduleConfiguration schedule(String appId) {
        ScheduleResponse scheduleResponse = wepApiClientService.get(scheduleUrl, appIdUrlVariable(appId),
                ScheduleResponse.class);
        return scheduleResponse != null ? scheduleResponse.body : null;
    }

    private Map<String, ?> appIdUrlVariable(String appId) {
        return Collections.singletonMap("appId", appId);
    }

    @Data
    private static final class ScheduleResponse {
        private ScheduleConfiguration body;
    }

    @Data
    public static final class ScheduleConfiguration {
        private String appId;
        private Integer offsetDays;
        private Set<String> targetEnv;
        // replay exclusion operations
        private Map<String, Set<String>> excludeOperationMap;
        private Integer sendMaxQps;
        // record inclusion operations
        private Set<String> includeServiceOperationSet;
        // record exclusion operations
        private Set<String> excludeServiceOperationSet;
    }

    @Data
    private static final class ApplicationResponse {
        private Application body;
    }

    @Data
    static final class Application {
        private String appId;
        private int features;
        private String groupName;
        private String groupId;
        private String agentVersion;
        private String agentExtVersion;
        private String appName;
        private String description;

        /**
         * java_web_service
         * nodeJs_Web_service
         */
        private String category;
        private String owner;
        private String organizationName;
        private Integer recordedCaseCount;

        /**
         * organization_id
         */
        private String organizationId;
    }
}