package com.arextest.schedule.extension.invoker;

import com.arextest.schedule.extension.invoker.ReplayExtensionInvoker;
import com.arextest.schedule.extension.invoker.ReplayInvocation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DubboInvocation implements ReplayInvocation {
    private String url;
    private Map<String, String> attachments;
    private String interfaceName;
    private String methodName;
    private List<String> parameterTypes;
    private List<Object> parameters;
    private ReplayExtensionInvoker invoker;
    private Map<String, Object> attributes = new HashMap<>();


    public DubboInvocation(String url, Map<String, String> attachments, String interfaceName, String methodName,
                           List<String> parameterTypes, List<Object> parameters, ReplayExtensionInvoker invoker) {
        this.url = url;
        this.attachments = attachments;
        this.parameters = parameters;
        this.parameterTypes = parameterTypes;
        this.invoker = invoker;
        this.interfaceName = interfaceName;
        this.methodName = methodName;
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
    public Object get(String key) {
        return attributes.get(key);
    }
}
