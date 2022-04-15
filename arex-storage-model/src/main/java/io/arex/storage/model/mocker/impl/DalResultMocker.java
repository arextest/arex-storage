package io.arex.storage.model.mocker.impl;

import io.arex.storage.model.annotations.FieldCompression;
import io.arex.storage.model.mocker.AbstractMocker;
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
    @FieldCompression
    private String response;
    private String typeName;
    private String keyHolder;
    private String methodName;
}