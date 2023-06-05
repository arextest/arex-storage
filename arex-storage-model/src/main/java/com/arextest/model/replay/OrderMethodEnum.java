package com.arextest.model.replay;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum OrderMethodEnum {
    ASCENDING(1),
    DESCENDING(2);

    private final int code;
}
