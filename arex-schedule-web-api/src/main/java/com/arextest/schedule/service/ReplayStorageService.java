package com.arextest.schedule.service;

import com.arextest.schedule.client.HttpWepServiceApiClient;
import jakarta.annotation.Resource;
import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ReplayStorageService {

  @Resource
  private HttpWepServiceApiClient client;

  @Value("${arex.storage.clearScene.url}")
  private String clearReplayScenesUrl;

  public void clearReplayScenes(String appId) {
    try {
      client.get(clearReplayScenesUrl + appId, new HashMap<>(), Void.class);
    } catch (Exception e) {
      LOGGER.error("Failed to clear replay scenes for app {}", appId, e);
    }
  }
}
