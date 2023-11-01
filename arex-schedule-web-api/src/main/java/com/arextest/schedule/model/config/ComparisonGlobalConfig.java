package com.arextest.schedule.model.config;


import com.arextest.schedule.common.CommonConstant;
import lombok.Data;

/**
 * Created by qzmo on 2023/07/17
 */
@Data
public class ComparisonGlobalConfig extends ReplayComparisonConfig {

  private Boolean skipAssemble;
  public static String dependencyKey(String planId) {
    return CommonConstant.COMPARE_GLOBAL_CONFIG_REDIS_KEY + planId;
  }

  public static ComparisonGlobalConfig empty() {
    ComparisonGlobalConfig config = new ComparisonGlobalConfig();
    config.fillCommonFields();
    config.skipAssemble = Boolean.TRUE;
    return config;
  }
}