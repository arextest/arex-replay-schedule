package com.arextest.schedule.model.invocation;

import com.arextest.schedule.extension.invoker.ReplayExtensionInvoker;
import com.arextest.schedule.extension.invoker.ReplayInvocation;
import lombok.Setter;
import org.apache.commons.collections4.MapUtils;

import java.util.HashMap;
import java.util.Map;

public class GeneralInvocation implements ReplayInvocation {
    protected String url;
    @Setter
    protected ReplayExtensionInvoker invoker;
    protected Map<String, Object> attributes = new HashMap<>();

    public GeneralInvocation(String url, Map<String, Object> attributes) {
        this.url = url;
        if (MapUtils.isNotEmpty(attributes)) {
            this.attributes.putAll(attributes);
        }
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public ReplayExtensionInvoker getInvoker() {
        return invoker;
    }

    @Override
    public void put(String key, Object value) {
        this.attributes.put(key, value);
    }

    @Override
    public <T> T get(String key, Class<T> clazz) {
        Object value = attributes.get(key);
        if (value == null) {
            return null;
        }
        if (!clazz.isAssignableFrom(value.getClass())) {
            throw new IllegalArgumentException("Invalid value type, expected " + clazz.getSimpleName()
                    + ", but got " + value.getClass().getSimpleName());
        }
        return clazz.cast(value);
    }

    @Override
    public Object get(String key) {
        return attributes.get(key);
    }
}
