package com.arextest.storage.utils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;

public class JsonUtil {

  public static boolean isJsonStr(String obj) {
    return StringUtils.isNotEmpty(obj) && obj.startsWith("{") && obj.endsWith("}");
  }

  public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
}
