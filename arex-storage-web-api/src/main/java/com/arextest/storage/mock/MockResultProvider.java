package com.arextest.storage.mock;

import com.arextest.model.mock.Mocker;
import com.arextest.model.mock.MockCategoryType;
import org.apache.commons.lang3.tuple.Pair;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author jmo
 * @since 2021/11/9
 */
public interface MockResultProvider {
    <T extends Mocker> boolean putRecordResult(MockCategoryType categoryType,String recordId, Iterable<T> values);

    <T extends Mocker> boolean putReplayResult(T value);

    byte[] getRecordResult(@NotNull Mocker mockItem, MockResultContext context);

    List<byte[]> getRecordResultList(MockCategoryType category, String recordId);

    List<byte[]> getReplayResultList(MockCategoryType category, String replayId);

    int replayResultCount(MockCategoryType category, String replayResultId);

    int recordResultCount(MockCategoryType category, String recordId);

    boolean removeRecordResult(MockCategoryType category, String recordId);

    boolean removeReplayResult(MockCategoryType category, String replayResultId);
}