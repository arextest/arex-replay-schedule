package com.arextest.schedule.utils;

import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.ReplayPlan;
import com.arextest.schedule.model.deploy.ServiceInstance;
import com.arextest.schedule.model.deploy.ServiceInstanceOperation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * @author jmo
 * @since 2021/10/12
 */
@Slf4j
public final class ReplayParentBinder {

    private static final String PROTOCOL = "Protocol";

    private static final String HTTP_PROTOCOL = "http";

    private static final String DUBBO_PROTOCOL = "dubbo";

    private static final String SOA_PROTOCOL = "soa";


    private ReplayParentBinder() {

    }

    public static void setupReplayActionParent(List<ReplayActionItem> replayActionItemList, ReplayPlan replayPlan) {
        if (CollectionUtils.isEmpty(replayActionItemList)) {
            return;
        }
        for (ReplayActionItem actionItem : replayActionItemList) {
            actionItem.setParent(replayPlan);
        }
    }

    public static void setupCaseItemParent(List<ReplayActionCaseItem> sourceItemList, ReplayActionItem parent) {
        if (CollectionUtils.isEmpty(sourceItemList)) {
            return;
        }
        for (ReplayActionCaseItem caseItem : sourceItemList) {
            caseItem.setCaseType(parent.getActionType());
            setParent(caseItem, parent);
        }
    }

    private static void setParent(ReplayActionCaseItem caseItem, ReplayActionItem parent) {
        if (Objects.isNull(caseItem) || Objects.isNull(parent)) {
            return;
        }

        ReplayActionItem clone = buildReplayActionItem(parent);

        if (Objects.isNull(clone)) {
            caseItem.setParent(parent);
            return;
        }

        List<ServiceInstance> instanceList = new ArrayList<>();
        List<ServiceInstance> oldInstanceList = clone.getTargetInstance();

        if (CollectionUtils.isEmpty(oldInstanceList)) {
            return ;
        }

        String protocol = caseItem.getTargetRequest().attributeAsString(PROTOCOL);
        if (StringUtils.isEmpty(protocol)) {
            instanceList = filterTargetInstance(oldInstanceList);
        } else if (SOA_PROTOCOL.equalsIgnoreCase(protocol)){
            instanceList = oldInstanceList.stream().filter(element -> element != null && HTTP_PROTOCOL.equalsIgnoreCase(element.getProtocol())).collect(Collectors.toList());
        } else if (DUBBO_PROTOCOL.equalsIgnoreCase(protocol)) {
            instanceList = oldInstanceList.stream().filter(element -> element != null && DUBBO_PROTOCOL.equalsIgnoreCase(element.getProtocol())).collect(Collectors.toList());
        }

        if (CollectionUtils.isNotEmpty(instanceList)) {
            clone.setTargetInstance(instanceList);
        }
        caseItem.setParent(clone);
    }

    private static ReplayActionItem buildReplayActionItem(ReplayActionItem parent) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        ReplayActionItem cloneReplayActionItem = null;
        List<ServiceInstance> targetInstance = new ArrayList<>(parent.getTargetInstance().size());
        try {
            String oldReplayActionItemString = objectMapper.writeValueAsString(parent);
            cloneReplayActionItem = objectMapper.readValue(oldReplayActionItemString, ReplayActionItem.class);
            cloneReplayActionItem.setParent(parent.getParent());
            cloneReplayActionItem.setMappedInstanceOperation(parent.getMappedInstanceOperation());
            if (CollectionUtils.isNotEmpty(parent.getTargetInstance())) {
                targetInstance.addAll(parent.getTargetInstance());
                cloneReplayActionItem.setTargetInstance(targetInstance);
            }
            cloneReplayActionItem.setSendRateLimiter(parent.getSendRateLimiter());
            cloneReplayActionItem.setSourceInstance(parent.getSourceInstance());
            cloneReplayActionItem.setCaseItemList(parent.getCaseItemList());
        } catch (JsonProcessingException e) {
            LOGGER.error("cloneReplayActionItem  error:{}", e.getMessage());
        }
        return cloneReplayActionItem;
    }

    private static List<ServiceInstance> filterTargetInstance(List<ServiceInstance> instances) {
        if (CollectionUtils.isEmpty(instances)) {
            return Collections.emptyList();
        }

        List<ServiceInstance> filterInstance= new ArrayList<>();
        Map<String, List<ServiceInstance>> collect = instances.stream().collect(Collectors.groupingBy(ServiceInstance::getIp));
        collect.forEach((k, v) -> {
            if (CollectionUtils.isNotEmpty(v) && v.size() > 1) {
                Optional<ServiceInstance> serviceInstance = v.stream().filter(i -> i != null && HTTP_PROTOCOL.equalsIgnoreCase(i.getProtocol())).findFirst();
                serviceInstance.ifPresent(filterInstance::add);
            }else if (CollectionUtils.isNotEmpty(v) && v.size() == 1){
                if (v.get(0) != null) {
                    filterInstance.add(v.get(0));
                }
            }
        });
        return filterInstance;
    }

}