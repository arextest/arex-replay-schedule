package com.arextest.schedule.plan.builder;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

/**
 * @author jmo
 * @since 2021/9/18
 */
@AllArgsConstructor
public class BuildPlanValidateResult {

  /**
   * 成功
   */
  public final static int SUCCESS = 0;

  /**
   * 挂起
   */
  public final static int APP_ID_SUSPENDED = 1;

  /**
   * 执行中
   */
  public final static int APP_ID_RUNNING = 2;

  /**
   * APPID不合法
   */
  public final static int APP_ID_NOT_FOUND = 3;

  /**
   * APPID下没有接口
   */
  public final static int REQUESTED_EMPTY_OPERATION = 4;

  /**
   * APPID下没有SOA服务
   */
  public final static int APP_ID_NOT_FOUND_SERVICE = 5;

  /**
   * 没有接口
   */
  public final static int REQUESTED_OPERATION_NOT_FOUND = 6;

  /**
   * 没有CASE
   */
  public final static int REQUESTED_OPERATION_NOT_FOUND_ANY_CASE = 7;

  /**
   * 不合法的数据源
   */
  public final static int UNSUPPORTED_CASE_SOURCE_TYPE = 8;

  /**
   * 时间范围不正确
   */
  public final static int REQUESTED_CASE_TIME_RANGE_UNSUPPORTED = 9;

  /**
   * source 环境不合法
   */
  public final static int REQUESTED_SOURCE_ENV_UNAVAILABLE = 10;

  /**
   * target 环境不合法
   */
  public final static int REQUESTED_TARGET_ENV_UNAVAILABLE = 11;

  /**
   * 没有找到可用service
   */
  public final static int SERVICE_NOT_FOUND = 12;


  /**
   * service没找到此operation
   */
  public final static int OPERATION_NOT_FOUND = 13;

  @Getter
  private final int codeValue;

  @Getter
  private final String remark;

  public static BuildPlanValidateResult createSuccess() {
    return create(SUCCESS, StringUtils.EMPTY);
  }

  public static BuildPlanValidateResult create(int codeValue, String remark) {
    return new BuildPlanValidateResult(codeValue, remark);
  }

  public boolean failure() {
    return this.codeValue != SUCCESS;
  }

}