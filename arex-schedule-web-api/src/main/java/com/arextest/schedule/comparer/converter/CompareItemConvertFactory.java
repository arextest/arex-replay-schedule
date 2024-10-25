package com.arextest.schedule.comparer.converter;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CompareItemConvertFactory {
  private final Map<String, CompareItemConverter> categoryConverters;
  @Resource
  private CompareItemConverter defaultCompareItemConverterImpl;

  public CompareItemConvertFactory(@Autowired List<CompareItemConverter> converters) {
    this.categoryConverters = converters.stream().filter(c -> StringUtils.isNotBlank(c.getCategoryName()))
        .collect(Collectors.toMap(CompareItemConverter::getCategoryName, Function.identity()));
  }

  public CompareItemConverter getConvert(String categoryName) {
    return categoryConverters.getOrDefault(categoryName, defaultCompareItemConverterImpl);
  }
}
