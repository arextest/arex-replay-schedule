package com.arextest.schedule.common;

/**
 * @author wildeslam.
 * @create 2024/3/26 17:12
 */
public class UrlUtil {

  public static String getParamFromUrl(String url, String param) {
    String[] params = url.split("\\?");
    if (params.length < 2) {
      return null;
    }
    String[] paramArray = params[1].split("&");
    for (String p : paramArray) {
      String[] kv = p.split("=");
      if (kv[0].equals(param)) {
        return kv[1];
      }
    }
    return null;
  }

  public static String appendParamToUrl(String url, String param, String value) {
    if (url.contains("?")) {
      return url + "&" + param + "=" + value;
    } else {
      return url + "?" + param + "=" + value;
    }
  }

}
