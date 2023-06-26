package com.arextest.schedule.extension.invoker;

import java.util.Map;

public interface ReplayInvocation {
    String getUrl();

    Map<String, Object> getAttributes();

    ReplayExtensionInvoker getInvoker();

    void put(String key, Object value);

    <T> T get(String key, Class<T> clazz);

    Object get(String key);
}
