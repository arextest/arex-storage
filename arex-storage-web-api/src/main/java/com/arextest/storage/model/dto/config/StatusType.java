package com.arextest.storage.model.dto.config;


/**
 * @author jmo
 * @since 2022/1/21
 */
public enum StatusType implements Feature {

    /**
     * enable replay
     */
    REPLAY,
    /**
     * enable record
     */
    RECORD,
    /**
     * The status is ok
     */
    NORMAL;
    private final int mask;

    StatusType() {
        mask = (1 << ordinal());
    }

    @Override
    public boolean enabledIn(int flags) {
        return (flags & mask) != 0;
    }

    @Override
    public int getMask() {
        return mask;
    }
}
