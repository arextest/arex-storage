package com.arextest.model.replay;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum SortingTypeEnum {
    ASCENDING(1),
    DESCENDING(-1);

    private final int code;

    public int getCode() {
        return code;
    }
}
