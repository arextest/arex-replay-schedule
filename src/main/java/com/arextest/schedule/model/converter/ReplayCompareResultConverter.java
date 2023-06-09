package com.arextest.schedule.model.converter;

import com.arextest.common.utils.SerializationUtils;
import com.arextest.diff.model.log.LogEntity;
import com.arextest.schedule.model.ReplayCompareResult;
import com.arextest.schedule.model.dao.mongodb.ReplayCompareResultCollection;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * Created by qzmo on 2023/06/05.
 */
@Mapper
public interface ReplayCompareResultConverter {

    ReplayCompareResultConverter INSTANCE = Mappers.getMapper(ReplayCompareResultConverter.class);


    @Mappings({
            @Mapping(target = "id", expression = "java(null)"),
            @Mapping(target = "dataChangeCreateTime", expression = "java(System.currentTimeMillis())"),
            @Mapping(target = "dataChangeUpdateTime", expression = "java(System.currentTimeMillis())"),
            @Mapping(target = "dataChangeCreateDate", expression = "java(new java.util.Date())")
    })
    ReplayCompareResultCollection daoFromDto(ReplayCompareResult dto);

    default String map(List<LogEntity> logs) {
        if (logs == null) {
            return StringUtils.EMPTY;
        }
        return SerializationUtils.useZstdSerializeToBase64(logs.toArray());
    }
}