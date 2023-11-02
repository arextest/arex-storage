package com.arextest.model.replay;

import lombok.AllArgsConstructor;

/**
 * @author wildeslam.
 * @create 2023/11/1 16:55
 */
@AllArgsConstructor
public enum AppVisibilityLevelEnum {
    // default.
    ALL_VISIBLE(0),
    OWNER_VISIBLE(1);

    private final int code;

    public int getCode() {
        return code;
    }

    public static boolean valid(int parameter) {
        for (AppVisibilityLevelEnum appVisibilityLevel : AppVisibilityLevelEnum.values()) {
            if (parameter == appVisibilityLevel.code) {
                return true;
            }
        }
        return false;
    }
}
