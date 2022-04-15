package io.arex.replay.schedule.service;

import io.arex.common.utils.CompressionUtils;
import io.arex.report.model.api.contracts.common.CompareResult;
import io.arex.report.model.api.contracts.common.NodeEntity;
import io.arex.replay.schedule.model.ReplayCompareResult;
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
        NodeEntity toNodeEntity(io.arex.diff.model.log.NodeEntity source);

        default List<NodeEntity> toNodeList(List<io.arex.diff.model.log.NodeEntity> source) {
            if (CollectionUtils.isEmpty(source)) {
                return Collections.emptyList();
            }
            List<NodeEntity> nodeListResult = new ArrayList<>(source.size());
            for (io.arex.diff.model.log.NodeEntity entity : source) {
                nodeListResult.add(toNodeEntity(entity));
            }
            return nodeListResult;
        }

        default List<List<NodeEntity>> nestedNodeList(List<List<io.arex.diff.model.log.NodeEntity>> source) {
            if (CollectionUtils.isEmpty(source)) {
                return null;
            }
            List<List<NodeEntity>> nestedResult = new ArrayList<>(source.size());
            for (List<io.arex.diff.model.log.NodeEntity> nodeList : source) {
                nestedResult.add(toNodeList(nodeList));
            }
            return nestedResult;
        }
    }

}
