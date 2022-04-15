package io.arex.storage.core.repository;

import io.arex.storage.model.replay.ReplayCaseRangeRequestType;
import io.arex.storage.model.mocker.ConfigVersion;
import io.arex.storage.model.mocker.MockItem;

/**
 * @author jmo
 * @since 2021/11/7
 */
public interface RepositoryReader<T extends MockItem> {
    Iterable<T> queryRecordList(String recordId);

    T queryRecord(String recordId);

    Iterable<T> queryReplayResult(String replayResultId);

    Iterable<T> queryByRange(ReplayCaseRangeRequestType rangeRequestType);

    T queryByVersion(ConfigVersion versionRequestType);

    int countByRange(ReplayCaseRangeRequestType rangeRequestType);
}
