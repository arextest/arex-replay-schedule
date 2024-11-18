package com.arextest.schedule.comparer.impl;

import com.arextest.diff.model.CompareOptions;
import com.arextest.diff.model.script.ScriptCompareConfig;
import com.arextest.diff.model.script.ScriptCompareConfig.ScriptMethod;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.schedule.comparer.CompareItem;
import com.arextest.schedule.comparer.CustomComparisonConfigurationHandler;
import com.arextest.schedule.model.ReplayActionItem;
import com.arextest.schedule.model.config.ComparisonDependencyConfig;
import com.arextest.schedule.model.config.ComparisonInterfaceConfig;
import com.arextest.schedule.model.config.ReplayComparisonConfig;
import com.arextest.schedule.model.converter.ScriptMethodConverter;
import com.arextest.schedule.model.converter.TransformConfigConverter;
import com.arextest.web.model.contract.contracts.config.replay.ComparisonSummaryConfiguration.ReplayScriptMethod;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

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
  public ReplayComparisonConfig pickConfig(ComparisonInterfaceConfig operationConfig,
      CompareItem compareItem, String category) {
    return this.pickConfig(operationConfig, category,
        compareItem.getCompareOperation());
  }

  @Override
  public ReplayComparisonConfig pickConfig(ComparisonInterfaceConfig operationConfig,
      String category, String operationName) {
    boolean mainEntryType = Optional.ofNullable(operationConfig.getOperationTypes())
        .map(types -> types.contains(category)).orElse(false);
    boolean mainEntryNameMatched = Optional.ofNullable(operationConfig.getOperationName())
        .map(name -> name.equals(operationName)).orElse(false);

    if (mainEntryType && mainEntryNameMatched) {
      return operationConfig;
    }
    ComparisonDependencyConfig defaultDependencyConfig = operationConfig.getDefaultDependencyConfig();
    ReplayComparisonConfig configWhenMissing =
        defaultDependencyConfig != null ? defaultDependencyConfig : new ReplayComparisonConfig();
    String depKey = ComparisonDependencyConfig.dependencyKey(category, operationName);
    Optional<ComparisonDependencyConfig> matchedDep = Optional.ofNullable(
            operationConfig.getDependencyConfigMap())
        .map(dependencyConfig -> dependencyConfig.get(depKey));
    // if not matched, use global config
    return matchedDep.isPresent() ? matchedDep.get() : configWhenMissing;
  }


  @Override
  public CompareOptions buildSkdOption(String category, ReplayComparisonConfig compareConfig) {
    CompareOptions options = new CompareOptions();
    options.putCategoryType(category);
    if (Objects.equals(category, MockCategoryType.DATABASE.getName())) {
      options.putExclusions(DEFAULT_DATABASE_IGNORE);
    }

    if (compareConfig != null) {
      options.putExclusions(compareConfig.getExclusionList());
      options.putInclusions(compareConfig.getInclusionList());
      options.putListSortConfig(compareConfig.getListSortMap());
      options.putReferenceConfig(compareConfig.getReferenceMap());
      options.putTransformConfig(
          Optional.ofNullable(compareConfig.getTransformDetails()).orElse(Collections.emptyList())
              .stream()
              .map(TransformConfigConverter.INSTANCE::toTransformConfig)
              .collect(Collectors.toList())
      );
      options.putScriptCompareConfig(convertToScriptMethod(compareConfig.getScriptMethodMap()));
    }
    return options;
  }

  protected List<ScriptCompareConfig> convertToScriptMethod(
      Map<List<String>, ReplayScriptMethod> scriptMethodMap) {
    if (MapUtils.isEmpty(scriptMethodMap)) {
      return Collections.emptyList();
    }

    List<ScriptCompareConfig> result = new ArrayList<>();
    for (Map.Entry<List<String>, ReplayScriptMethod> entry : scriptMethodMap.entrySet()) {
      List<String> nodePath = entry.getKey();
      ReplayScriptMethod scriptMethod = entry.getValue();
      if (CollectionUtils.isEmpty(nodePath) || scriptMethod == null) {
        continue;
      }
      ScriptCompareConfig scriptCompareConfig = new ScriptCompareConfig();
      ScriptMethod sdkScriptMethod = ScriptMethodConverter.INSTANCE.toScriptMethod(scriptMethod);
      scriptCompareConfig.setNodePath(nodePath);
      scriptCompareConfig.setScriptMethod(sdkScriptMethod);
      result.add(scriptCompareConfig);
    }
    return result;
  }

}
