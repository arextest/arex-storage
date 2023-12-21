package com.arextest.storage.model;

import lombok.Data;

/**
 * Invalid case request type
 * @author: sldu
 * @date: 2023/11/27 16:10
 **/
@Data
public class InvalidIncompleteRecordRequest {
    private String appId;
    private String recordId;
    /**
     * FastReject or QueueOverFlow
     */
    private String reason;
    /**
     * Replay id
     * replay scene must be not null
     */
    private String replayId;

    @Override
    public String toString() {
        return "InvalidIncompleteRecordRequest{" +
                "appId='" + appId + '\'' +
                ", recordId='" + recordId + '\'' +
                ", reason=" + reason +
                ", replayId='" + replayId + '\'' +
                '}';
    }
}
