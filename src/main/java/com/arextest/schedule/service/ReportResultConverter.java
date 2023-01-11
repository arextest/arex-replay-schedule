package com.arextest.schedule.service;

import com.arextest.common.utils.CompressionUtils;
import com.arextest.report.model.api.contracts.common.CompareResult;
import com.arextest.report.model.api.contracts.common.NodeEntity;
import com.arextest.schedule.model.ReplayCompareResult;
import org.apache.commons.collections4.CollectionUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author jmo
 * @since 2022/1/28
 */
@Mapper(imports = CompressionUtils.class, uses = ReportResultConverter.NodeEntityConverter.class)
public interface ReportResultConverter {
    ReportResultConverter DEFAULT = Mappers.getMapper(ReportResultConverter.class);

    @Mapping(expression = "java(CompressionUtils.useZstdCompress(source.getBaseMsg()))", target = "baseMsg")
    @Mapping(expression = "java(CompressionUtils.useZstdCompress(source.getTestMsg()))", target = "testMsg")
    CompareResult to(ReplayCompareResult source);

    @Mapper
    interface NodeEntityConverter {
        NodeEntity toNodeEntity(com.arextest.diff.model.log.NodeEntity source);

        default List<NodeEntity> toNodeList(List<com.arextest.diff.model.log.NodeEntity> source) {
            if (CollectionUtils.isEmpty(source)) {
                return Collections.emptyList();
            }
            List<NodeEntity> nodeListResult = new ArrayList<>(source.size());
            for (com.arextest.diff.model.log.NodeEntity entity : source) {
                nodeListResult.add(toNodeEntity(entity));
            }
            return nodeListResult;
        }

        default List<List<NodeEntity>> nestedNodeList(List<List<com.arextest.diff.model.log.NodeEntity>> source) {
            if (CollectionUtils.isEmpty(source)) {
                return null;
            }
            List<List<NodeEntity>> nestedResult = new ArrayList<>(source.size());
            for (List<com.arextest.diff.model.log.NodeEntity> nodeList : source) {
                nestedResult.add(toNodeList(nodeList));
            }
            return nestedResult;
        }
    }

}