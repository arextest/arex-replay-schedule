package com.arextest.schedule.model.converter;

import com.arextest.common.utils.SerializationUtils;
import com.arextest.diff.model.MsgInfo;
import com.arextest.diff.model.log.LogEntity;
import com.arextest.schedule.model.ReplayCompareResult;
import com.arextest.schedule.model.dao.mongodb.ReplayCompareMsgInfoCollection;
import com.arextest.schedule.model.dao.mongodb.ReplayCompareResultCollection;
import com.arextest.schedule.model.report.CompareResultDetail;
import com.arextest.web.model.contract.contracts.replay.AnalyzeCompareResultsRequestType;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;

import java.util.Arrays;
import java.util.List;

/**
 * Created by qzmo on 2023/06/05.
 */
@Mapper(componentModel = "spring")
public abstract class ReplayCompareResultConverter extends DesensitizationConverter {
    @Mappings({
            @Mapping(target = "dataChangeCreateTime", expression = "java(System.currentTimeMillis())"),
            @Mapping(target = "dataChangeUpdateTime", expression = "java(System.currentTimeMillis())"),
            @Mapping(target = "dataChangeCreateDate", expression = "java(new java.util.Date())"),
            @Mapping(target = "baseMsg", qualifiedByName = "encryptAndCompress"),
            @Mapping(target = "testMsg", qualifiedByName = "encryptAndCompress"),
            @Mapping(target = "logs", qualifiedByName = "compressLogs"),
    })
    public abstract ReplayCompareResultCollection daoFromBo(ReplayCompareResult bo);

    @Mappings({
            @Mapping(target = "baseMsg", qualifiedByName = "decompressAndDecrypt"),
            @Mapping(target = "testMsg", qualifiedByName = "decompressAndDecrypt"),
            @Mapping(target = "logs", qualifiedByName = "decompressLogs")
    })
    public abstract ReplayCompareResult boFromDao(ReplayCompareResultCollection dao);
    public abstract CompareResultDetail voFromBo(ReplayCompareResult bo);
    public abstract AnalyzeCompareResultsRequestType.AnalyzeCompareInfoItem reportContractFromBo(ReplayCompareResult source);

    @Named("compressLogs")
    String map(List<LogEntity> logs) {
        if (logs == null) {
            return StringUtils.EMPTY;
        }
        return SerializationUtils.useZstdSerializeToBase64(logs.toArray());
    }

    @Named("decompressLogs")
    List<LogEntity> map(String logs) {
        LogEntity[] logEntities = SerializationUtils.useZstdDeserialize(logs, LogEntity[].class);
        if (logEntities == null) {
            return null;
        }
        return Arrays.asList(logEntities);
    }

    @Named("msgInfo")
    ReplayCompareMsgInfoCollection convertMsg(MsgInfo msgInfo) {
        ReplayCompareMsgInfoCollection ret = new ReplayCompareMsgInfoCollection();
        ret.setMsgMiss(msgInfo.getMsgMiss());
        return ret;
    }

    @Named("msgInfo")
    MsgInfo convertMsg(ReplayCompareMsgInfoCollection source) {
        MsgInfo ret = new MsgInfo();
        ret.setMsgMiss(source.getMsgMiss());
        return ret;
    }
}