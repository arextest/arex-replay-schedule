package com.arextest.schedule.utils;

import java.util.Base64;
import java.util.regex.Pattern;

/**
 * @author wildeslam.
 * @create 2023/11/30 20:43
 */
public class DecodeUtils {
  private static final String PATTERN_STRING = "^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)?$";
  private static final Pattern BASE_64_PATTERN = Pattern.compile(PATTERN_STRING);

  public static Object decode(String requestMessage) {
    if (BASE_64_PATTERN.matcher(requestMessage).matches()) {
      return Base64.getDecoder().decode(requestMessage);
    }
    return requestMessage;
  }
}
