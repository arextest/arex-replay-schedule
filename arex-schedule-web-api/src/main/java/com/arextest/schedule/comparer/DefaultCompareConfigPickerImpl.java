package com.arextest.schedule.comparer;

import com.arextest.schedule.model.config.ComparisonDependencyConfig;
import com.arextest.schedule.model.config.ComparisonGlobalConfig;
import com.arextest.schedule.model.config.ComparisonInterfaceConfig;
import com.arextest.schedule.model.config.ReplayComparisonConfig;

import java.util.Optional;

/**
 * Created by Qzmo on 2023/8/2
 */
public class DefaultCompareConfigPickerImpl implements CompareConfigPicker {
    public ReplayComparisonConfig pickConfig(ComparisonGlobalConfig globalConfig,
                                                    ComparisonInterfaceConfig operationConfig,
                                                    CompareItem compareItem, String category) {
        if (compareItem.isEntryPointCategory()) {
            return operationConfig;
        }

        String depKey = ComparisonDependencyConfig.dependencyKey(category, compareItem.getCompareOperation());
        Optional<ComparisonDependencyConfig> matchedDep = Optional.ofNullable(operationConfig.getDependencyConfigMap())
                .map(dependencyConfig -> dependencyConfig.get(depKey));
        return matchedDep.isPresent() ? matchedDep.get() : globalConfig;
    }

    public ReplayComparisonConfig pickConfig(ComparisonGlobalConfig globalConfig, ComparisonInterfaceConfig operationConfig,
                                                    String category, String operationName) {
        boolean mainEntryType = Optional.ofNullable(operationConfig.getOperationTypes())
                .map(types -> types.contains(category)).orElse(false);
        boolean mainEntryNameMatched = Optional.ofNullable(operationConfig.getOperationName())
                .map(name -> name.equals(operationName)).orElse(false);

        if (mainEntryType && mainEntryNameMatched) {
            return operationConfig;
        }

        String depKey = ComparisonDependencyConfig.dependencyKey(category, operationName);
        Optional<ComparisonDependencyConfig> matchedDep = Optional.ofNullable(operationConfig.getDependencyConfigMap())
                .map(dependencyConfig -> dependencyConfig.get(depKey));

        // if not matched, use global config
        return matchedDep.isPresent() ? matchedDep.get() : globalConfig;
    }
}
