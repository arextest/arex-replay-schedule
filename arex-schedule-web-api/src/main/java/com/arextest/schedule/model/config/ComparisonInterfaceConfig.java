package com.arextest.schedule.model.config;


import com.arextest.schedule.common.CommonConstant;
import java.util.Map;
import lombok.Data;

/**
 * Created by qzmo on 2023/07/17ComparisonInterfaceConfig
 */
@Data
public class ComparisonInterfaceConfig extends ReplayComparisonConfig {

  private ComparisonDependencyConfig defaultDependencyConfig;

  private Map<String, ComparisonDependencyConfig> dependencyConfigMap;

  public static String dependencyKey(String actionId) {
    return CommonConstant.COMPARE_CONFIG_REDIS_KEY + actionId;
  }

  public static ComparisonInterfaceConfig empty() {
    ComparisonInterfaceConfig config = new ComparisonInterfaceConfig();
    config.fillCommonFields();
    return config;
  }
}