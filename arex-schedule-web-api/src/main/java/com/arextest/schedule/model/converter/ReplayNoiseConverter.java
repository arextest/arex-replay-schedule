package com.arextest.schedule.model.converter;

import com.arextest.schedule.model.dao.mongodb.ReplayNoiseCollection;
import com.arextest.schedule.model.noiseidentify.QueryNoiseResponseType;
import com.arextest.schedule.model.noiseidentify.ReplayNoiseDto;
import com.arextest.schedule.model.noiseidentify.ReplayNoiseItemDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

/**
 * Created by coryhh on 2023/10/17.
 */
@Mapper
public interface ReplayNoiseConverter {

  ReplayNoiseConverter INSTANCE = Mappers.getMapper(ReplayNoiseConverter.class);

  ReplayNoiseCollection daoFromDto(ReplayNoiseDto dto);

  ReplayNoiseDto dtoFromDao(ReplayNoiseCollection dao);

  @Mapping(target = "compareResultId",
      expression = "java(dto == null || dto.getCompareResult() == null ? null : dto.getCompareResult().getId())")
  ReplayNoiseCollection.ReplayNoiseItemDao daoFromDto(ReplayNoiseItemDto dto);

  ReplayNoiseItemDto dtoFromDao(ReplayNoiseCollection.ReplayNoiseItemDao dao);

  @Mappings(
      @Mapping(target = "nodeEntity", source = "nodePath")
  )
  QueryNoiseResponseType.NoiseItem toNoiseItem(ReplayNoiseItemDto dto);
}
