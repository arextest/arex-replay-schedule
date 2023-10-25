package com.arextest.schedule.comparer.impl;

import com.arextest.diff.model.CompareOptions;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.schedule.comparer.CompareItem;
import com.arextest.schedule.comparer.CustomComparisonConfigurationHandler;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.config.ComparisonDependencyConfig;
import com.arextest.schedule.model.config.ComparisonGlobalConfig;
import com.arextest.schedule.model.config.ComparisonInterfaceConfig;
import com.arextest.schedule.model.config.ReplayComparisonConfig;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @Author wang_yc
 * @Date 2023/5/25 22:31
 */
public class DefaultCustomComparisonConfigurationHandler implements
    CustomComparisonConfigurationHandler {

  private static final List<String> DEFAULT_DATABASE_IGNORE = Collections.singletonList("body");


  @Override
  public void build(ReplayComparisonConfig config, ReplayActionItem actionItem) {
    // if you want to add custom comparison configuration, you can do it here.
  }

  @Override
  public ReplayComparisonConfig pickConfig(ComparisonGlobalConfig globalConfig,
      ComparisonInterfaceConfig operationConfig,
      CompareItem compareItem, String category) {
    if (compareItem.isEntryPointCategory()) {
      return operationConfig;
    }

    String depKey = ComparisonDependencyConfig.dependencyKey(category,
        compareItem.getCompareOperation());
    Optional<ComparisonDependencyConfig> matchedDep = Optional.ofNullable(
            operationConfig.getDependencyConfigMap())
        .map(dependencyConfig -> dependencyConfig.get(depKey));
    return matchedDep.isPresent() ? matchedDep.get() : globalConfig;
  }

  @Override
  public ReplayComparisonConfig pickConfig(ComparisonGlobalConfig globalConfig,
      ComparisonInterfaceConfig operationConfig,
      String category, String operationName) {
    boolean mainEntryType = Optional.ofNullable(operationConfig.getOperationTypes())
        .map(types -> types.contains(category)).orElse(false);
    boolean mainEntryNameMatched = Optional.ofNullable(operationConfig.getOperationName())
        .map(name -> name.equals(operationName)).orElse(false);

    if (mainEntryType && mainEntryNameMatched) {
      return operationConfig;
    }

    String depKey = ComparisonDependencyConfig.dependencyKey(category, operationName);
    Optional<ComparisonDependencyConfig> matchedDep = Optional.ofNullable(
            operationConfig.getDependencyConfigMap())
        .map(dependencyConfig -> dependencyConfig.get(depKey));

    // if not matched, use global config
    return matchedDep.isPresent() ? matchedDep.get() : globalConfig;
  }


  @Override
  public CompareOptions buildSkdOption(String category, ReplayComparisonConfig compareConfig) {
    CompareOptions options = new CompareOptions();
    options.putCategoryType(category);
    // todo: the switch of "sqlBodyParse" and "onlyCompareCoincidentColumn"
    //  need get from ReplayComparisonConfig
    options.putSelectIgnoreCompare(true);
    options.putOnlyCompareCoincidentColumn(true);
    options.putExclusions(compareConfig.getExclusionList());
    options.putInclusions(compareConfig.getInclusionList());
    options.putListSortConfig(compareConfig.getListSortMap());
    options.putReferenceConfig(compareConfig.getReferenceMap());

    if (Objects.equals(category, MockCategoryType.DATABASE.getName())) {
      options.putExclusions(DEFAULT_DATABASE_IGNORE);
    }
    return options;
  }
}
