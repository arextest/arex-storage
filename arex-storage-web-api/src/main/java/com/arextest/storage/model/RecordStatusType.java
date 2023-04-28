package com.arextest.storage.model;

import lombok.Getter;

/**
 * @author xinyuan_wang
 * @since 2023/05/09
 */
public enum RecordStatusType {
    /**
     * matching mocks that have already been used during replay mock
     */
    USED(0),
    /**
     * a mock that has not yet been used during replay mock
     */
    UNUSED(1);
    @Getter
    private final int codeValue;

    RecordStatusType(int codeValue) {
        this.codeValue = codeValue;
    }
}