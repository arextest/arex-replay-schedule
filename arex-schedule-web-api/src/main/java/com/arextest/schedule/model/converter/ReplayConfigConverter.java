package com.arextest.schedule.model.converter;

import com.arextest.schedule.model.config.ComparisonDependencyConfig;
import com.arextest.schedule.model.config.ComparisonGlobalConfig;
import com.arextest.schedule.model.config.ComparisonInterfaceConfig;
import com.arextest.web.model.contract.contracts.config.replay.ComparisonSummaryConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * Created by qzmo on 2023/5/31.
 */
@Mapper
public interface ReplayConfigConverter {

    ReplayConfigConverter INSTANCE = Mappers.getMapper(ReplayConfigConverter.class);

    ComparisonInterfaceConfig interfaceDaoFromDto(ComparisonSummaryConfiguration dto);
    ComparisonGlobalConfig globalDaoFromDto(ComparisonSummaryConfiguration dto);
    ComparisonDependencyConfig dependencyDaoFromDto(ComparisonSummaryConfiguration dto);
}