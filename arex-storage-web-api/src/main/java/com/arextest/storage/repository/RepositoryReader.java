package com.arextest.storage.repository;

import com.arextest.storage.model.mocker.ConfigVersion;
import com.arextest.storage.model.mocker.MockItem;
import com.arextest.storage.model.replay.ReplayCaseRangeRequestType;

/**
 * @author jmo
 * @since 2021/11/7
 */
public interface RepositoryReader<T extends MockItem> {
    Iterable<T> queryRecordList(String recordId);

    T queryRecord(String recordId);

    Iterable<T> queryByRange(ReplayCaseRangeRequestType rangeRequestType);

    T queryByVersion(ConfigVersion versionRequestType);

    int countByRange(ReplayCaseRangeRequestType rangeRequestType);
}