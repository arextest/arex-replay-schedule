package com.arextest.replay.schedule.model.converter;

import com.arextest.replay.schedule.model.ReplayActionCaseItem;
import com.arextest.replay.schedule.model.dao.mongodb.ReplayRunDetailsCollection;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

/**
 * Created by rchen9 on 2022/8/19.
 */
@Mapper
public interface ReplayRunDetailsConverter {

    ReplayRunDetailsConverter INSTANCE = Mappers.getMapper(ReplayRunDetailsConverter.class);

    ReplayActionCaseItem dtoFromDao(ReplayRunDetailsCollection dao);


    @Mappings({
            @Mapping(target = "id", expression = "java(null)"),
            @Mapping(target = "operationId", expression = "java(null)"),
            @Mapping(target = "dataChangeCreateTime", expression = "java(System.currentTimeMillis())"),
            @Mapping(target = "dataChangeUpdateTime", expression = "java(System.currentTimeMillis())")
    })
    ReplayRunDetailsCollection daoFromDto(ReplayActionCaseItem dto);
}
