


package com.arextest.schedule.utils;

import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.ReplayPlan;
import com.arextest.schedule.model.converter.ReplayPlanItemConverter;
import com.arextest.schedule.model.deploy.ServiceInstance;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import java.util.*;
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

    private static final String DUBBO_PROVIDER = "DubboProvider";

    private static final String SOA_PROVIDER = "SoaProvider";


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

        Map<String, List<ReplayActionCaseItem>> collect = new HashMap<>();
        for (ReplayActionCaseItem item : sourceItemList) {
            collect.computeIfAbsent(item.getCaseType(), k -> new ArrayList<>()).add(item);
        }

        for (List<ReplayActionCaseItem> itemList : collect.values()) {
            ReplayActionItem replayActionItem = buildParent(itemList.get(0), parent);
            for (ReplayActionCaseItem replayActionCaseItem : itemList) {
                replayActionCaseItem.setParent(replayActionItem);
            }
        }
    }

    private static ReplayActionItem buildParent(ReplayActionCaseItem caseItem, ReplayActionItem parent) {
        Objects.requireNonNull(caseItem);
        Objects.requireNonNull(parent);
        if (StringUtils.isBlank(caseItem.getCaseType())) {
            return parent;
        }
        if (!DUBBO_PROVIDER.equalsIgnoreCase(caseItem.getCaseType()) && !SOA_PROVIDER.equalsIgnoreCase(caseItem.getCaseType())) {
            return parent;
        }

        ReplayActionItem clone = ReplayPlanItemConverter.INSTANCE.clone(parent);

        List<ServiceInstance> instanceList = new ArrayList<>();
        List<ServiceInstance> oldInstanceList = clone.getTargetInstance();
        String protocol = caseItem.getTargetRequest().attributeAsString(PROTOCOL);
        if (SOA_PROTOCOL.equalsIgnoreCase(protocol)) {
            instanceList = filterInstancesByProtocol(oldInstanceList, HTTP_PROTOCOL);
        } else if (DUBBO_PROTOCOL.equalsIgnoreCase(protocol)) {
            instanceList = filterInstancesByProtocol(oldInstanceList, DUBBO_PROTOCOL);
        } else {
            instanceList = filterTargetInstance(oldInstanceList);
        }

        if (CollectionUtils.isNotEmpty(instanceList)) {
            clone.setTargetInstance(instanceList);
        }
        return clone;
    }

    private static List<ServiceInstance> filterTargetInstance(List<ServiceInstance> instances) {
        Map<String, ServiceInstance> filteredInstances = new HashMap<>();
        for (ServiceInstance instance : instances) {
            if (instance == null) {
                continue;
            }
            String ip = instance.getIp();
            String protocol = instance.getProtocol();
            if (StringUtils.isBlank(ip) || StringUtils.isBlank(protocol)) {
                continue;
            }
            if (filteredInstances.containsKey(ip) && HTTP_PROTOCOL.equalsIgnoreCase(protocol)) {
                filteredInstances.put(ip, instance);
            } else if (!filteredInstances.containsKey(ip)) {
                filteredInstances.put(ip, instance);
            }
        }
        return new ArrayList<>(filteredInstances.values());
    }

    private static List<ServiceInstance> filterInstancesByProtocol(List<ServiceInstance> instances, String protocol) {
        Objects.requireNonNull(instances);
        Objects.requireNonNull(protocol);
        List<ServiceInstance> result = new ArrayList<>();
        for (ServiceInstance instance : instances) {
            if (protocol.equalsIgnoreCase(instance.getProtocol())) {
                result.add(instance);
            }
        }
        return result;
    }

}