package com.arextest.model.replay;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum OrderMethod {
    ASCENDING(1),
    DESCENDING(2);

    private Integer code;
}
