package com.arextest.storage.model.enums;

import lombok.Getter;

/**
 * @author jmo
 * @since 2021/11/18
 */
public enum RecordEnvType {
    /**
     * production
     */
    PRO(0),
    /**
     * testing
     */
    TEST(1);
    @Getter
    private final int codeValue;

    RecordEnvType(int codeValue) {
        this.codeValue = codeValue;
    }
}