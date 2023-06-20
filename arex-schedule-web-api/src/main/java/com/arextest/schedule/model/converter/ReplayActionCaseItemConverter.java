package com.arextest.schedule.model.converter;

import com.arextest.schedule.spi.model.BaseRequest;
import com.arextest.schedule.spi.model.DubboRequest;
import com.arextest.schedule.spi.model.ReplayActionCaseItem;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ReplayActionCaseItemConverter {

    ReplayActionCaseItemConverter INSTANCE = Mappers.getMapper(ReplayActionCaseItemConverter.class);

    ReplayActionCaseItem convertCaseItem(com.arextest.schedule.model.ReplayActionCaseItem replayActionCaseItem);

    DubboRequest convertDubboRequest(com.arextest.schedule.model.ReplayActionCaseItem replayActionCaseItem);

    BaseRequest convertBaseRequest(com.arextest.schedule.model.ReplayActionCaseItem replayActionCaseItem);

}
