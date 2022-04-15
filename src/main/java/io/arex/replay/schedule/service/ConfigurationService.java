package io.arex.replay.schedule.service;

import io.arex.replay.schedule.client.HttpWepServiceApiClient;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * @author jmo
 * @since 2022/2/17
 */
@Service
public final class ConfigurationService {
    @Resource
    private HttpWepServiceApiClient wepApiClientService;
    @Value("${arex.config.application.url}")
    private String applicationUrl;
    @Value("${arex.config.schedule.url}")
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
        /**
         * 默认回放Case范围
         */
        private Integer offsetDays;

        /**
         * 默认回放环境
         */
        private Set<String> targetEnv;

        private Integer sendMaxQps;

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
