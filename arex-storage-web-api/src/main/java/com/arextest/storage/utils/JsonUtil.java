package com.arextest.storage.utils;

import org.apache.commons.lang3.StringUtils;

public class JsonUtil {
    public static boolean isJsonStr(String obj) {
        return StringUtils.isNotEmpty(obj) && obj.startsWith("{") && obj.endsWith("}");
    }
}
