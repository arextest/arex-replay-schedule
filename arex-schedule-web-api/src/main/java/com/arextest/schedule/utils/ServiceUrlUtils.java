package com.arextest.schedule.utils;

import org.apache.commons.lang3.StringUtils;

import java.net.URI;

/**
 * the service url utitls
 *
 * @author thji
 * @date 2024/8/1
 * @since 1.0.0
 */
public class ServiceUrlUtils {

    private ServiceUrlUtils() {
    }

    /**
     * get the host from the url
     *
     * @param url e.g. http://test:8080, dubbo://dubbo:20700...
     * @return the host
     */
    public static String getHost(String url) {
        if (StringUtils.isBlank(url)) {
            return null;
        }
        return URI.create(url).getHost();
    }
}
