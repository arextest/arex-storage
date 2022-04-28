package com.arextest.storage.model.mocker.impl;

import com.arextest.storage.model.mocker.AbstractMocker;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author jmo
 * @since 2021/11/2
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ABtMocker extends AbstractMocker {
    /**
     * abt的key
     */
    private String expCode;

    /**
     * abt的value
     */
    private String version;
}