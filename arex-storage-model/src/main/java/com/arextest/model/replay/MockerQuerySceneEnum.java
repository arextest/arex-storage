package com.arextest.model.replay;

import org.apache.commons.lang3.StringUtils;

public enum MockerQuerySceneEnum {
    /**
     * NONE, do not exclude any fields
     */
    NORMAL("NONE"),
    /**
     * Exclude targetResponse field
     */
    EXCLUDE_RESPONSE("EXCLUDE_RESPONSE", "targetResponse");

    private final String name;
    private final String[] excludeFields;

    MockerQuerySceneEnum(String name, String... excludeFields) {
        this.name = name;
        this.excludeFields = excludeFields;
    }


    public String getName() {
        return name;
    }

    public String[] getExcludeFields() {
        return excludeFields;
    }

    public static MockerQuerySceneEnum fromName(String name) {
        if (StringUtils.isBlank(name)) {
            return NORMAL;
        }
        if (NORMAL.getName().equals(name)) {
            return NORMAL;
        }
        if (EXCLUDE_RESPONSE.getName().equals(name)) {
            return EXCLUDE_RESPONSE;
        }
        return NORMAL;
    }
}
