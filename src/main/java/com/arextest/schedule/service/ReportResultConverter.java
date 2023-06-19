package com.arextest.schedule.service;

import com.arextest.common.utils.CompressionUtils;
import com.arextest.web.model.contract.contracts.common.CompareResult;
import com.arextest.web.model.contract.contracts.common.NodeEntity;
import com.arextest.schedule.model.ReplayCompareResult;
import com.arextest.web.model.contract.contracts.replay.AnalyzeCompareResultsRequestType;
import org.apache.commons.collections4.CollectionUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author jmo
 * @since 2022/1/28
 */
@Mapper
public interface ReportResultConverter {
    ReportResultConverter DEFAULT = Mappers.getMapper(ReportResultConverter.class);

    AnalyzeCompareResultsRequestType.AnalyzeCompareInfoItem to(ReplayCompareResult source);
}