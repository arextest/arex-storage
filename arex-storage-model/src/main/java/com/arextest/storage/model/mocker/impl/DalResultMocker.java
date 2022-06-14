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
public class DalResultMocker extends AbstractMocker {
    private String database;
    private String sql;
    private String parameter;
    private String typeName;
    private String keyHolder;
    private String methodName;
}