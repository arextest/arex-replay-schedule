package com.arextest.replay.schedule.comparer;

import com.arextest.common.cache.CacheProvider;
import com.arextest.replay.schedule.client.HttpWepServiceApiClient;
import com.arextest.replay.schedule.model.ReplayActionItem;
import com.arextest.replay.schedule.model.ReplayPlan;
import com.arextest.replay.schedule.model.config.CompareConfigDetail;
import com.arextest.replay.schedule.model.config.CompareIgnoreConfig;
import com.arextest.replay.schedule.model.config.ReplayComparisonConfig;
import com.arextest.replay.schedule.utils.FormatUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.arextest.replay.schedule.common.CommonConstant;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by wang_yc on 2021/10/14
 */
@Slf4j
@Component
public final class CompareConfigService {
    @Resource
    private HttpWepServiceApiClient httpWepServiceApiClient;
    @Resource
    private CacheProvider redisCacheProvider;
    @Value("${arex.config.comparison.url}")
    private String configComparisonUrl;
    @Resource
    private ObjectMapper objectMapper;

    public void preload(ReplayPlan plan) {
        Map<String, String> urlVariables = Collections.singletonMap("appId", plan.getAppId());
        GenericResponseType responseType = httpWepServiceApiClient.get(configComparisonUrl, urlVariables,
                GenericResponseType.class);
        if (responseType == null || CollectionUtils.isEmpty(responseType.body)) {
            LOGGER.warn("prepare load appId comparison config empty");
            return;
        }
        List<CompareIgnoreConfig> source = responseType.body;
        for (ReplayActionItem actionItem : plan.getReplayActionItemList()) {
            long operationId = actionItem.getOperationId();
            ReplayComparisonConfig config = build(source, operationId);
            String redisKey = key(actionItem.getId());
            String json = objectToJsonString(config);
            redisCacheProvider.put(redisKey.getBytes(StandardCharsets.UTF_8),
                    7200L, json.getBytes(StandardCharsets.UTF_8));
            LOGGER.info("prepare load compare config, action id:{} ,result: {}", actionItem.getId(), json);
        }
    }

    private ReplayComparisonConfig build(List<CompareIgnoreConfig> source, long operation) {
        ReplayComparisonConfig replayComparisonConfig = newEmptyComparisonConfig();
        long configOperationId;
        for (CompareIgnoreConfig config : source) {
            configOperationId = config.getOperationId();
            if (configOperationId == operation || configOperationId == 0) {
                expandComparisonConfig(replayComparisonConfig, config);
            }
        }
        return replayComparisonConfig;
    }

    @Data
    private final static class GenericResponseType {
        private List<CompareIgnoreConfig> body;
    }

    public ReplayComparisonConfig loadConfig(ReplayActionItem actionItem) {
        try {
            String redisKey = key(actionItem.getId());
            byte[] json = redisCacheProvider.get(redisKey.getBytes(StandardCharsets.UTF_8));
            if (json == null) {
                return newEmptyComparisonConfig();
            }
            ReplayComparisonConfig config = byteToObject(json, ReplayComparisonConfig.class);
            if (config == null) {
                return newEmptyComparisonConfig();
            }
            return config;
        } catch (Throwable throwable) {
            LOGGER.error(throwable.getMessage(), throwable);
        }
        return newEmptyComparisonConfig();
    }

    private <T> T byteToObject(byte[] bytes, Class<T> tClass) {
        try {
            return objectMapper.readValue(bytes, tClass);
        } catch (IOException e) {
            LOGGER.error("byteToObject error:{}", e.getMessage(), e);
        }
        return null;
    }

    private String objectToJsonString(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (IOException e) {
            LOGGER.error("byteToObject error:{}", e.getMessage(), e);
        }
        return StringUtils.EMPTY;
    }

    private String key(long actionId) {
        return CommonConstant.COMPARE_CONFIG_REDIS_KEY + actionId;
    }

    private void expandComparisonConfig(ReplayComparisonConfig target,
                                        CompareIgnoreConfig source) {
        List<CompareConfigDetail> configDetailList = source.getDetailsList();
        if (CollectionUtils.isEmpty(configDetailList)) {
            return;
        }
        switch (source.getCategoryType()) {
            case 0:
                target.setIgnorePathList(FormatUtil.toIgnoreList(configDetailList));
                break;
            case 1:
                Set<String> ignoreNodeSet = new HashSet<>();
                for (CompareConfigDetail configDetail : configDetailList) {
                    ignoreNodeSet.addAll(configDetail.getPathValue());
                }
                target.setIgnoreNodeList(ignoreNodeSet);
                break;
            case 2:
                List<String> ignoreTypeList = new ArrayList<>();
                for (CompareConfigDetail configDetail : configDetailList) {
                    ignoreTypeList.addAll(configDetail.getPathValue());
                }
                target.setIgnoreTypeList(ignoreTypeList);
                break;
            case 3:
                List<String> ignoreKeyList = new ArrayList<>();
                for (CompareConfigDetail configDetail : configDetailList) {
                    ignoreKeyList.addAll(configDetail.getPathValue());
                }
                target.setIgnoreKeyList(ignoreKeyList);
                break;
            case 4:
                target.setListKeyList(FormatUtil.toSortKeys(configDetailList));
                break;
            case 5:
                target.setReferenceList(FormatUtil.toReferenceMap(configDetailList));
                break;
            case 6:
                target.setDecompressConfig(FormatUtil.toDecompressMap(configDetailList));
                break;
            case 7:
                target.setInclusionList(FormatUtil.toIgnoreList(configDetailList));
                break;
            default:
                break;
        }

    }

    private ReplayComparisonConfig newEmptyComparisonConfig() {
        ReplayComparisonConfig replayComparisonConfig = new ReplayComparisonConfig();
        replayComparisonConfig.setIgnoreKeyList(Collections.emptyList());
        replayComparisonConfig.setIgnoreNodeList(Collections.emptySet());
        replayComparisonConfig.setIgnorePathList(Collections.emptyList());
        replayComparisonConfig.setIgnoreTypeList(Collections.emptyList());
        replayComparisonConfig.setListKeyList(Collections.emptyMap());
        replayComparisonConfig.setReferenceList(Collections.emptyMap());
        return replayComparisonConfig;
    }
}
