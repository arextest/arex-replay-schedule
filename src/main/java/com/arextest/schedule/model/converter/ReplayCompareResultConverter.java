package com.arextest.schedule.model.converter;

import com.arextest.common.utils.SerializationUtils;
import com.arextest.diff.model.MsgInfo;
import com.arextest.diff.model.log.LogEntity;
import com.arextest.schedule.model.ReplayCompareResult;
import com.arextest.schedule.model.dao.mongodb.ReplayCompareMsgInfoCollection;
import com.arextest.schedule.model.dao.mongodb.ReplayCompareResultCollection;
import com.arextest.schedule.utils.ZstdUtils;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.Arrays;
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
            @Mapping(target = "dataChangeCreateDate", expression = "java(new java.util.Date())"),
            @Mapping(target = "baseMsg", qualifiedByName = "compressMsg"),
            @Mapping(target = "testMsg", qualifiedByName = "compressMsg"),
    })
    ReplayCompareResultCollection daoFromDto(ReplayCompareResult dto);

    default String map(List<LogEntity> logs) {
        if (logs == null) {
            return StringUtils.EMPTY;
        }
        return SerializationUtils.useZstdSerializeToBase64(logs.toArray());
    }

    @Named("msgInfo")
    default ReplayCompareMsgInfoCollection convertMsg(MsgInfo msgInfo) {
        ReplayCompareMsgInfoCollection ret = new ReplayCompareMsgInfoCollection();
        ret.setMsgMiss(msgInfo.getMsgMiss());
        return ret;
    }

    @Named("compressMsg")
    default String compressMsg(String decompressString) {
        return ZstdUtils.compressString(decompressString);
    }

    @Named("decompressMsg")
    default String decompressMsg(String compressString) {
        return ZstdUtils.uncompressString(compressString);
    }

}