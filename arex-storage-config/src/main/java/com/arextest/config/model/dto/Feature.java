package com.arextest.config.model.dto;

/**
 * @author jmo
 * @since 2022/1/21
 */
public interface Feature {
    String name();

    boolean enabledIn(int flags);

    int getMask();
}
