package io.arex.replay.schedule.sender;

import java.util.Map;

/**
 * @author: miaolu
 * @create: 2021-12-08
 **/
public interface SenderParameters {
    String getAppId();

    String getUrl();

    String getOperation();

    String getMessage();

    String getFormat();

    String getSubEnv();

    String getConsumeGroup();

    /**
     * default http post
     *
     * @return the method to sending
     */
    default String getMethod() {
        return "POST";
    }

    default String getRecordId() {
        return null;
    }

    Map<String, String> getHeaders();
}
