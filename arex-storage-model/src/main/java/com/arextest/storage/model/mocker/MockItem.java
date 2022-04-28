package com.arextest.storage.model.mocker;

/**
 * @author jmo
 * @since 2021/11/8
 */
public interface MockItem {
    String getReplayId();

    void setReplayId(String replayId);

    String getRecordId();

    void setRecordId(String recordId);

    /**
     * millis from utc format without timezone
     */
    void setCreateTime(long createTime);
}
