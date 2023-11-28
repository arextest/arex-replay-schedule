package com.arextest.schedule.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * @author wildeslam.
 * @create 2023/11/23 16:20
 */
@Slf4j
public class JsonUtils {
  private static final ObjectMapper objectMapper = new ObjectMapper();

  public static  <T> T byteToObject(byte[] bytes, Class<T> tClass) {
    try {
      return objectMapper.readValue(bytes, tClass);
    } catch (IOException e) {
      LOGGER.error("byteToObject error:{}", e.getMessage(), e);
    }
    return null;
  }

  public static String objectToJsonString(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (IOException e) {
      LOGGER.error("byteToObject error:{}", e.getMessage(), e);
    }
    return StringUtils.EMPTY;
  }
}
