package com.arextest.storage.model;

import com.arextest.storage.model.header.ResponseStatusType;

/**
 * @author jmo
 * @since 2021/11/3
 */
public interface Response {
    ResponseStatusType getResponseStatusType();

    void setResponseStatusType(ResponseStatusType responseStatusType);
}