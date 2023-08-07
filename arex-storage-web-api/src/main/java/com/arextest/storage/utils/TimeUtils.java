package com.arextest.storage.utils;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * @author wildeslam.
 * @created 2023/8/3 20:12
 */
public class TimeUtils {
    public static final long ONE_DAY = 24 * 60 * 60 * 1000L;
    public static final long ONE_HOUR = 60 * 60 * 1000L;

    public static long getTodayFirstMills() {
        return LocalDate.now().atStartOfDay().withSecond(0).withNano(0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
