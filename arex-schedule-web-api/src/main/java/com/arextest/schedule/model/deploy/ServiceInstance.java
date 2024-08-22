package com.arextest.schedule.model.deploy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;

import com.arextest.schedule.common.SendSemaphoreLimiter;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

/**
 * json example :
 * <code>
 * "ip": "10.128.5.4", "machineName": "r100016523-81001165-mqdj2", "port": 8080, "status": "up",
 * "url": "http://10.128.5.4:8080/api/", "protocol": "http", "zoneId": "ntgxh", "serviceId":
 * "flight.ticket.refund.refundpaymentservice.v1.refundpaymentservice", "metadata": { "appId":
 * "100016523", "subEnv": "uat" }
 * </code>
 *
 * @author jmo
 * @since 2021/9/18
 */
@Data
public class ServiceInstance {

  public final static String DUBBO_PROTOCOL = "dubbo";
  public final static String HTTP_PROTOCOL = "http";
  public final static String UP_STATUS = "up";
  private String ip;
  private Integer port;
  private String url;
  private String machineName;
  private String tag;

  /**
   * "dubbo" or "http"
   */
  private String protocol;
  /**
   * "status": "up" or "down"
   */
  private String status;
  private String serviceId;
  private String contextPath;
  private List<ServiceInstanceOperation> operationList;
  private Env metadata;

  @JsonIgnore
  @Getter
  @Setter
  private SendSemaphoreLimiter sendSemaphoreLimiter;

  public String subEnv() {
    return metadata == null ? StringUtils.EMPTY : metadata.subEnv;
  }

  @Getter
  @Setter
  public static class Env {

    private String subEnv;
  }

}