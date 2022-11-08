package com.arextest.storage.model.mocker;


import com.arextest.storage.model.annotations.FieldCompression;
import lombok.Data;

/**
 * @author jmo
 * @since 2021/11/2
 */
@Data
public abstract class AbstractMocker implements MockItem {
    private String id;
    private String replayId;
    private String recordId;
    private String appId;
    private long createTime;
    private String ip;
    @FieldCompression
    private String response;
    private String responseType;
}