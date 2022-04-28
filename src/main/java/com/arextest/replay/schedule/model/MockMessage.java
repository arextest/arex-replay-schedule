package com.arextest.replay.schedule.model;

import java.util.Collections;
import java.util.Map;

/**
 * @author jmo
 * @since 2021/9/23
 */
public interface MockMessage {
    int SOA_TRIGGER = 0;
    int QMQ_TRIGGER = 10;
    int INTERNAL_DEPENDENCY_SOA = 1;
    int INTERNAL_PRODUCE_QMQ = 2;
    int INTERNAL_PROCESS_DB = 3;
    int INTERNAL_PROCESS_REDIS = 4;
    int INTERNAL_PROCESS_DYNAMIC = 5;
    int INTERNAL_DEPENDENCY_HTTP = 6;
    int INTERNAL_ABTEST_VERSION = 7;

    String getAppId();

    String getService();

    String getOperation();

    String getRequest();

    String getResponse();

    String getCompareMessage();


    // from  configVersion
    String getReplayDependence();

    String getConsumerGroupName();

    String getFormat();

    String getCaseId();

    String getReplayId();

    long getCreateTime();

    default Map<String, Object> getTags() {
        return Collections.emptyMap();
    }

    default int getCategoryType() {
        return SOA_TRIGGER;
    }
}
