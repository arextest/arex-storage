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
public class HttpClientMocker extends AbstractMocker {
    private String contentType;
    private String url;
    @FieldCompression
    private String request;
    @FieldCompression
    private String response;
    private String responseType;
}
