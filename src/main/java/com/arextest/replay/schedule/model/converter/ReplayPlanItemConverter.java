package com.arextest.replay.schedule.model.converter;

import com.arextest.replay.schedule.model.ReplayActionItem;
import com.arextest.replay.schedule.model.dao.mongodb.ReplayPlanItemCollection;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

/**
 * Created by rchen9 on 2022/8/19.
 */
@Mapper
public interface ReplayPlanItemConverter {

    ReplayPlanItemConverter INSTANCE = Mappers.getMapper(ReplayPlanItemConverter.class);

    ReplayActionItem dtoFromDao(ReplayPlanItemCollection dao);

    @Mappings({
            @Mapping(target = "id", expression = "java(null)"),
            @Mapping(target = "dataChangeCreateTime", expression = "java(System.currentTimeMillis())"),
            @Mapping(target = "dataChangeUpdateTime", expression = "java(System.currentTimeMillis())")
    })
    ReplayPlanItemCollection daoFromDto(ReplayActionItem dto);

}
