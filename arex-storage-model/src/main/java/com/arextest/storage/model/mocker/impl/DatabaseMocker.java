package com.arextest.storage.model.mocker.impl;

import com.arextest.storage.model.annotations.FieldCompression;
import com.arextest.storage.model.mocker.AbstractMocker;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author yongwuhe
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class DatabaseMocker extends AbstractMocker {
    private String dbName;
    private String parameters;
    private String tables;
    private String sql;
    private String keyHolder;
    private String methodName;
}