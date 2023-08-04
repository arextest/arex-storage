package com.arextest.storage.utils;

/**
 * @author wildeslam.
 * @created 2023/8/3 20:12
 */
public class TimeUtils {
    public static final long ONE_DAY = 24 * 60 * 60 * 1000L;
    public static final long ONE_HOUR = 60 * 60 * 1000L;
    public static final int ZERO_TIMESTAMP_HOURS = 8;

    public static long getLastMillis(long timestamp) {
        return timestamp / ONE_DAY * ONE_DAY + ONE_DAY - 1 - ZERO_TIMESTAMP_HOURS * ONE_HOUR;
    }
}
