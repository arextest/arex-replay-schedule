package com.arextest.schedule.comparer;

import java.util.Base64;

public class EncodingUtils {

  public static String base64decode(String encoded) {
    try {
      // to-do: 64base extract record and result
      if (encoded == null) {
        return null;
      }
      if (isJson(encoded)) {
        return encoded;
      }
      String decoded = new String(Base64.getDecoder().decode(encoded));
      if (isJson(decoded)) {
        return decoded;
      }
      return encoded;
    } catch (Exception e) {
      return encoded;
    }
  }

  private static boolean isJson(String value) {
    if (value.startsWith("{") && value.endsWith("}")) {
      return true;
    } else {
      return value.startsWith("[") && value.endsWith("]");
    }
  }
}
