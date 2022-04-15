package io.arex.storage.model.mocker;


import lombok.Data;

/**
 * @author jmo
 * @since 2021/11/2
 */
@Data
public abstract class AbstractMocker implements MockItem {
    private String replayId;
    private String recordId;
    private String appId;
    private long createTime;
    private String ip;
}