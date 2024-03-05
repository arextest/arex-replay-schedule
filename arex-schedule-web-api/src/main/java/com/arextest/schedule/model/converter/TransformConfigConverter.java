package com.arextest.schedule.model.converter;

import com.arextest.diff.model.TransformConfig;
import com.arextest.web.model.contract.contracts.compare.TransformDetail;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

@Mapper
public interface TransformConfigConverter {

  TransformConfigConverter INSTANCE = Mappers.getMapper(TransformConfigConverter.class);

  @Mappings({
      @Mapping(target = "nodePath", expression = "java(transformDetail.getNodePath() == null ? "
          + "null : java.util.Collections.singletonList(transformDetail.getNodePath()))"),
  })
  TransformConfig toTransformConfig(TransformDetail transformDetail);

}
