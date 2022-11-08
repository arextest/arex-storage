package com.arextest.storage.service.utils;

/**
 * Created by rchen9 on 2022/10/19.
 */
public class MockCategoryUtils {

    public static boolean shouldSkipCategory(long categoryTypes, int codeValue) {
        return (categoryTypes > 0 && (categoryTypes & 1L << codeValue) == 0);
    }

}