package com.arextest.schedule.service;

import com.arextest.config.model.dto.system.DesensitizationJar;
import com.arextest.model.response.ResponseStatusType;
import com.arextest.schedule.client.HttpWepServiceApiClient;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Resource;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.RetryException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
public class ConfigurationService {

  @Resource
  private HttpWepServiceApiClient wepApiClientService;
  @Value("${arex.api.config.application.url}")
  private String applicationUrl;
  @Value("${arex.api.config.schedule.url}")
  private String scheduleUrl;
  @Value("${arex.api.config.desensitization.url}")
  private String desensitizationConfigUrl;

  public Application application(String appId) {
    ApplicationResponse applicationResponse = wepApiClientService.get(applicationUrl,
        appIdUrlVariable(appId),
        ApplicationResponse.class);
    return applicationResponse != null ? applicationResponse.body : null;
  }

  public ScheduleConfiguration schedule(String appId) {
    ScheduleResponse scheduleResponse = wepApiClientService.get(scheduleUrl,
        appIdUrlVariable(appId),
        ScheduleResponse.class);
    return scheduleResponse != null ? scheduleResponse.body : null;
  }

  @Retryable(value = {RetryException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
  public List<DesensitizationJar> desensitization() {
    DesensitizationResponse res = wepApiClientService.jsonPost(desensitizationConfigUrl, null,
        DesensitizationResponse.class);
    if (res == null) {
      throw new RetryException("get desensitization config error");
    } else {
      return res.getBody();
    }
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
  private static final class DesensitizationResponse {

    private ResponseStatusType responseStatusType;
    private List<DesensitizationJar> body;
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
     * java_web_service nodeJs_Web_service
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