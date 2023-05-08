package com.arextest.schedule.model.converter;

import com.arextest.schedule.model.ReplayActionItem;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ReplayActionItemConverter {

    ReplayActionItemConverter INSTANCE = Mappers.getMapper(ReplayActionItemConverter.class);

    ReplayActionItem clone(ReplayActionItem replayActionItem);


}
