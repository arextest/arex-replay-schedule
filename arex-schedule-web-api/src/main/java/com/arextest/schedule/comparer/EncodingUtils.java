package com.arextest.schedule.comparer;

import java.util.Base64;

/**
 * Created by qzmo on 2023-11-27
 * <p>
 * Extracted from DefaultReplayResultComparer
 */
public class EncodingUtils {

  public static String tryBase64Decode(String encoded) {
    try {
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
