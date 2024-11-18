package com.arextest.schedule.model.converter;

import com.arextest.diff.model.script.ScriptCompareConfig;
import com.arextest.web.model.contract.contracts.config.replay.ComparisonSummaryConfiguration.ReplayScriptMethod;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ScriptMethodConverter {

  ScriptMethodConverter INSTANCE = Mappers.getMapper(ScriptMethodConverter.class);

  ScriptCompareConfig.ScriptMethod toScriptMethod(ReplayScriptMethod replayScriptMethod);

}
