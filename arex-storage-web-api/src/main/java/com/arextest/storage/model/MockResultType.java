package com.arextest.storage.model;

import lombok.Getter;

/**
 * @author jmo
 * @since 2021/11/7
 */
public enum MockResultType {

    /**
     * The pure origin record from the agent's sample
     */
    RECORD_RESULT(0),

    /**
     * based on origin record do after replay result
     */
    REPLAY_RESULT(1),
    /**
     * The sequence of mock return when replaying
     */
    CONSUME_RESULT(2),

    /**
     * a mapping relation between recordId and replayId
     */
    RECORD_REPLAY_MAPPING(3),

    /**
     * the id of record mock
     */
    RECORD_INSTANCE_ID(4);


    @Getter
    private final int codeValue;

    MockResultType(int codeValue) {
        this.codeValue = codeValue;
    }
}