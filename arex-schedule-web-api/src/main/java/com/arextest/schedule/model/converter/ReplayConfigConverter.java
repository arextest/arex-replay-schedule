package com.arextest.schedule.model.converter;

import com.arextest.schedule.model.config.ComparisonDependencyConfig;
import com.arextest.schedule.model.config.ComparisonInterfaceConfig;
import com.arextest.web.model.contract.contracts.config.replay.ComparisonSummaryConfiguration;
import com.arextest.web.model.contract.contracts.config.replay.ReplayCompareConfig;
import com.arextest.web.model.contract.contracts.config.replay.ReplayCompareConfig.DependencyComparisonItem;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by qzmo on 2023/5/31.
 */
@Mapper
public interface ReplayConfigConverter {


  Logger LOGGER = LoggerFactory.getLogger(ReplayConfigConverter.class);

  ReplayConfigConverter INSTANCE = Mappers.getMapper(ReplayConfigConverter.class);

  @Mappings({
      @Mapping(target = "dependencyConfigMap", source = "dependencyComparisonItems", qualifiedByName = "toDependencyConfigMap"),
      @Mapping(target = "defaultDependencyConfig", source = "defaultDependencyComparisonItem", qualifiedByName = "dependencyDaoFromDto")
  })
  ComparisonInterfaceConfig interfaceDaoFromDto(ReplayCompareConfig.ReplayComparisonItem dto);

  @Named("dependencyDaoFromDto")
  ComparisonDependencyConfig dependencyDaoFromDto(ComparisonSummaryConfiguration dto);


  @Named("toDependencyConfigMap")
  default Map<String, ComparisonDependencyConfig> toDependencyConfigMap(
      List<DependencyComparisonItem> dependencyComparisonItems) {
    Map<String, ComparisonDependencyConfig> res = new HashMap<>();

    for (ReplayCompareConfig.DependencyComparisonItem source : dependencyComparisonItems) {
      if (CollectionUtils.isEmpty(source.getOperationTypes()) || StringUtils.isBlank(
          source.getOperationName())) {
        LOGGER.warn("dependency type or name is blank, dependencyId: {}", source.getDependencyId());
        continue;
      }

      String dependencyKey = ComparisonDependencyConfig.dependencyKey(source);
      ComparisonDependencyConfig converted = dependencyDaoFromDto(source);
      res.put(dependencyKey, converted);
    }

    return res;
  }
}