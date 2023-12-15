package com.arextest.schedule.model.converter;

import com.arextest.schedule.model.dao.mongodb.ExpectationResultCollection;
import com.arextest.schedule.model.expectation.ExpectationResultModel;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * @since 2023/12/17
 */
@Mapper
public interface ExpectationMapper {
    ExpectationMapper INSTANCE = Mappers.getMapper(ExpectationMapper.class);
    ExpectationResultModel toModel(ExpectationResultCollection collection);
}
