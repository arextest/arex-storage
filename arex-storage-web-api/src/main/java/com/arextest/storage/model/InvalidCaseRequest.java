package com.arextest.storage.model;

import lombok.Data;

/**
 * Invalid case request type
 * @author: sldu
 * @date: 2023/11/27 16:10
 **/
@Data
public class InvalidCaseRequest {
    private String appId;
    private String recordId;
    /**
     * FastReject or QueueOverFlow
     */
    private String reason;

    @Override
    public String toString() {
        return "InvalidCaseRequest{" +
                "appId='" + appId + '\'' +
                ", recordId='" + recordId + '\'' +
                ", reason=" + reason +
                '}';
    }
}
