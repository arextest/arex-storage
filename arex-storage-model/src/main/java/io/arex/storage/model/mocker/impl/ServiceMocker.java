package io.arex.storage.model.mocker.impl;

import io.arex.storage.model.annotations.FieldCompression;
import io.arex.storage.model.mocker.AbstractMocker;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * defined a remote web service
 *
 * @author jmo
 * @since 2021/11/2
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ServiceMocker extends AbstractMocker {
    private String service;
    private String operation;
    @FieldCompression
    private String request;
    @FieldCompression
    private String response;
}