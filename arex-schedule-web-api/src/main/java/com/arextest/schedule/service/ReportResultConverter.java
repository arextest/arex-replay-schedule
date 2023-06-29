package com.arextest.schedule.service;

import com.arextest.schedule.model.ReplayCompareResult;
import com.arextest.web.model.contract.contracts.replay.AnalyzeCompareResultsRequestType;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * @author jmo
 * @since 2022/1/28
 */
@Mapper
public interface ReportResultConverter {
    ReportResultConverter DEFAULT = Mappers.getMapper(ReportResultConverter.class);
    AnalyzeCompareResultsRequestType.AnalyzeCompareInfoItem to(ReplayCompareResult source);
}