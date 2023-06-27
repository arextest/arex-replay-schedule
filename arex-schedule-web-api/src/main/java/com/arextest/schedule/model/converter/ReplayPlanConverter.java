package com.arextest.schedule.model.converter;

import com.arextest.schedule.model.ReplayPlan;
import com.arextest.schedule.model.dao.mongodb.ReplayPlanCollection;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

/**
 * Created by rchen9 on 2022/8/19.
 */
@Mapper
public interface ReplayPlanConverter {

    ReplayPlanConverter INSTANCE = Mappers.getMapper(ReplayPlanConverter.class);

    ReplayPlan dtoFromDao(ReplayPlanCollection dao);


    @Mappings({
            @Mapping(target = "id", expression = "java(null)"),
            @Mapping(target = "dataChangeCreateTime", expression = "java(System.currentTimeMillis())"),
            @Mapping(target = "dataChangeUpdateTime", expression = "java(System.currentTimeMillis())"),
            @Mapping(target = "dataChangeCreateDate", expression = "java(new java.util.Date())")
    })
    ReplayPlanCollection daoFromDto(ReplayPlan dto);

}