package com.arextest.schedule;

import java.util.List;
import java.util.Map;

public interface DubboInvoker {
    String getName();

    ReplayInvokeResult invoke(String url, Map<String, String> headers, String interfaceName, String methodName,
                  List<String> parameterTypes, List<Object> parameters);
}
