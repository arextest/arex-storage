package com.arextest.model.replay;

import org.apache.commons.lang3.StringUtils;

public enum MockerProjectionEnum {
    /**
     * NONE, do not exclude any fields
     */
    NONE("NONE"),
    /**
     * Exclude targetResponse field
     */
    EXCLUDE_RESPONSE("EXCLUDE_RESPONSE", "targetResponse");

    private final String name;
    private final String[] excludeFields;

    MockerProjectionEnum(String name, String... excludeFields) {
        this.name = name;
        this.excludeFields = excludeFields;
    }


    public String getName() {
        return name;
    }

    public String[] getExcludeFields() {
        return excludeFields;
    }

    public static MockerProjectionEnum fromName(String name) {
        if (StringUtils.isBlank(name)) {
            return NONE;
        }
        if (NONE.getName().equals(name)) {
            return NONE;
        }
        if (EXCLUDE_RESPONSE.getName().equals(name)) {
            return EXCLUDE_RESPONSE;
        }
        return NONE;
    }
}
