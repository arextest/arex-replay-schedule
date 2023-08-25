package com.arextest.schedule.model.converter;

import com.arextest.common.utils.SerializationUtils;
import com.arextest.model.mock.Mocker;
import com.arextest.schedule.model.ReplayActionCaseItem;
import com.arextest.schedule.model.dao.mongodb.ReplayRunDetailsCollection;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;
import org.springframework.core.convert.converter.Converter;

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

    String encryptAndCompressReq(Mocker.Target req) {
        Mocker.Target cloned = new Mocker.Target();
        cloned.setBody(encrypt(req.getBody()));
        cloned.setAttributes(req.getAttributes());
        cloned.setType(req.getType());
        return SerializationUtils.useZstdSerializeToBase64(cloned);
    }

    Mocker.Target decompressAndDecryptReq(String req) {
        Mocker.Target decompress = SerializationUtils.useZstdDeserialize(req, Mocker.Target.class);
        decompress.setBody(decrypt(decompress.getBody()));
        return decompress;
    }
}