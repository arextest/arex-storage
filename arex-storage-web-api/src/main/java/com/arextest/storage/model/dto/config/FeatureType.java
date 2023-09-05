package com.arextest.storage.model.dto.config;

/**
 * @author jmo
 * @since 2022/1/21
 */
public enum FeatureType implements Feature {

    /**
     * enable continuous integration
     */
    CI;
    private final int mask;

    FeatureType() {
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
