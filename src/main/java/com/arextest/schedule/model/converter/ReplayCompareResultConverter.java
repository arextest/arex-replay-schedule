package com.arextest.schedule.model.converter;

import com.arextest.schedule.model.ReplayCompareResult;
import com.arextest.schedule.model.ReplayPlan;
import com.arextest.schedule.model.dao.mongodb.ReplayCompareResultCollection;
import com.arextest.schedule.model.dao.mongodb.ReplayPlanCollection;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

/**
 * Created by qzmo on 2023/06/05.
 */
@Mapper
public interface ReplayCompareResultConverter {

    ReplayCompareResultConverter INSTANCE = Mappers.getMapper(ReplayCompareResultConverter.class);


    @Mappings({
            @Mapping(target = "id", expression = "java(null)"),
            @Mapping(target = "dataChangeCreateTime", expression = "java(System.currentTimeMillis())"),
            @Mapping(target = "dataChangeUpdateTime", expression = "java(System.currentTimeMillis())")
    })
    ReplayCompareResultCollection daoFromDto(ReplayCompareResult dto);
}