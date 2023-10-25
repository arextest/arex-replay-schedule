package com.arextest.schedule.model.config;


import com.arextest.web.model.contract.contracts.config.replay.ReplayCompareConfig;
import lombok.Data;

/**
 * Created by qzmo on 2023/07/17
 */
@Data
public class ComparisonDependencyConfig extends ReplayComparisonConfig {

  public static String dependencyKey(
      ReplayCompareConfig.DependencyComparisonItem dependencyConfig) {
    return dependencyKey(dependencyConfig.getOperationTypes().get(0),
        dependencyConfig.getOperationName());
  }

  public static String dependencyKey(String type, String name) {
    return type + "_" + name;
  }
}