package com.arextest.schedule.model.converter;

import com.arextest.schedule.model.bizlog.BizLog;
import com.arextest.schedule.model.dao.mongodb.ReplayBizLogCollection;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

/**
 * Created by qzmo on 2023/5/31.
 */
@Mapper
public interface ReplayBizLogConverter {

    ReplayBizLogConverter INSTANCE = Mappers.getMapper(ReplayBizLogConverter.class);

    @Mappings({
            @Mapping(target = "id", expression = "java(null)"),
            @Mapping(target = "dataChangeCreateTime", expression = "java(System.currentTimeMillis())"),
            @Mapping(target = "dataChangeUpdateTime", expression = "java(System.currentTimeMillis())"),
            @Mapping(target = "dataChangeCreateDate", expression = "java(new java.util.Date())")
    })
    ReplayBizLogCollection daoFromDto(BizLog dto);
}