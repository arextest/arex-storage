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
public class HttpClientMocker extends AbstractMocker {
    private String contentType;
    private String url;
    @FieldCompression
    private String request;
    @FieldCompression
    private String response;
    private String responseType;
}
