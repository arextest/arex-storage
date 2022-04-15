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
public class RedisMocker extends AbstractMocker {
    private String clusterName;
    private String redisKey;
    @FieldCompression
    private String operationResult;
    private String resultClazz;
}