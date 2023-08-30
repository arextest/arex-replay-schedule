package com.arextest.schedule.model.converter;

import com.arextest.common.utils.SerializationUtils;
import com.arextest.model.mock.Mocker;
import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.model.dao.mongodb.ReplayRunDetailsCollection;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

/**
 * Created by rchen9 on 2022/8/19.
 */
@Mapper(componentModel = "spring")
public abstract class ReplayRunDetailsConverter extends DesensitizationConverter {
    public abstract ReplayActionCaseItem dtoFromDao(ReplayRunDetailsCollection dao);

    @Mappings({
            @Mapping(target = "id", expression = "java(null)"),
            @Mapping(target = "operationId", expression = "java(null)"),
            @Mapping(target = "dataChangeCreateTime", expression = "java(System.currentTimeMillis())"),
            @Mapping(target = "dataChangeUpdateTime", expression = "java(System.currentTimeMillis())"),
            @Mapping(target = "dataChangeCreateDate", expression = "java(new java.util.Date())"),
    })
    public abstract ReplayRunDetailsCollection daoFromDto(ReplayActionCaseItem dto);

    String compressRequest(Mocker.Target req) {
        return encrypt(SerializationUtils.useZstdSerializeToBase64(req));
    }

    Mocker.Target decompressRequest(String req) {
        return SerializationUtils.useZstdDeserialize(decrypt(req), Mocker.Target.class);
    }
}