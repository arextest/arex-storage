package com.arextest.storage.constants;

import com.arextest.storage.model.enums.MockCategoryType;

/**
 * Created by rchen9 on 2022/10/11.
 */
public class MockCategoryMaskConstants {
    public static final long MAIN_CATEGORY_TYPES = mainCategoryMasks();

    private static long mainCategoryMasks() {
        long mainCategoryValue = 0;
        for (MockCategoryType categoryType : MockCategoryType.values()) {
            if (categoryType.isMainEntry()) {
                mainCategoryValue |= 1 << categoryType.getCodeValue();
            }
        }
        return mainCategoryValue;
    }
}