package com.arextest.schedule.model.invocation;

import com.arextest.schedule.extension.invoker.InvokerConstants;
import com.arextest.schedule.extension.invoker.ReplayExtensionInvoker;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Setter;

public class DubboInvocation extends GeneralInvocation {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private Map<String, String> attachments;
  private String interfaceName;
  private String methodName;
  private List<String> parameterTypes;
  private List<Object> parameters;

  @Setter
  @JsonIgnore
  private ReplayExtensionInvoker invoker;


  public DubboInvocation(String url, Map<String, String> attachments, String interfaceName,
      String methodName,
      List<String> parameterTypes, List<Object> parameters, ReplayExtensionInvoker invoker) {
    super(url, new HashMap<>());
    this.url = url;
    this.attachments = attachments;
    this.parameters = parameters;
    this.parameterTypes = parameterTypes;
    this.invoker = invoker;
    this.interfaceName = interfaceName;
    this.methodName = methodName;

    attributes.put(InvokerConstants.DUBBO_METHOD_NAME, methodName);
    attributes.put(InvokerConstants.DUBBO_INTERFACE_NAME, interfaceName);
    attributes.put(InvokerConstants.HEADERS, attachments);
    attributes.put(InvokerConstants.DUBBO_PARAMETERS, parameters);
    attributes.put(InvokerConstants.DUBBO_PARAMETER_TYPES, parameterTypes);
  }

  public DubboInvocation(String url, Map<String, String> attachments, String interfaceName,
      String methodName,
      List<String> parameterTypes, List<Object> parameters) {
    this(url, attachments, interfaceName, methodName, parameterTypes, parameters, null);
  }

  @Override
  public String toString() {
    try {
      return MAPPER.writeValueAsString(this);
    } catch (JsonProcessingException e) {
      return "transform json failed, errorMsg:" + e.getMessage();
    }
  }
}
