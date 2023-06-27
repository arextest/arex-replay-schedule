package com.arextest.schedule.extension.invoker;

import java.util.Map;

public interface ReplayInvocation {
    /**
     * eg: dubbo:127.0.0.1:20880
     * protocol + host + port
     */
    String getUrl();

    Map<String, Object> getAttributes();

    ReplayExtensionInvoker getInvoker();

    void put(String key, Object value);

    /**
     * Get specified class item from attributes.
     * key: refer to InvokerConstants.
     */
    <T> T get(String key, Class<T> clazz);

    /**
     * Get object item from attributes.
     * key: refer to InvokerConstants.
     */
    Object get(String key);
}
