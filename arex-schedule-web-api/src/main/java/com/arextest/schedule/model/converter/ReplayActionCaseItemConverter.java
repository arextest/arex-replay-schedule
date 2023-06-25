package com.arextest.schedule.model.converter;

import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.spi.model.BaseRequest;
import com.arextest.schedule.spi.model.DubboRequest;
import com.arextest.schedule.spi.model.ReplayInvokeRequest;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ReplayActionCaseItemConverter {

    ReplayActionCaseItemConverter INSTANCE = Mappers.getMapper(ReplayActionCaseItemConverter.class);

    ReplayInvokeRequest convertCaseItem(ReplayActionCaseItem replayActionCaseItem);

    DubboRequest convertDubboRequest(ReplayActionCaseItem replayActionCaseItem);

    BaseRequest convertBaseRequest(ReplayActionCaseItem replayActionCaseItem);

}
