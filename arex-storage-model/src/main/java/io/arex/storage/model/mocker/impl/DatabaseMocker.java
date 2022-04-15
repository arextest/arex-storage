package io.arex.storage.model.mocker.impl;

import io.arex.storage.model.annotations.FieldCompression;
import io.arex.storage.model.mocker.AbstractMocker;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author yongwuhe
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class DatabaseMocker extends AbstractMocker {
    private String dbName;
    @FieldCompression
    private String response;
    private String responseType;
    private String parameters;
    private String tables;
    private String sql;
}
