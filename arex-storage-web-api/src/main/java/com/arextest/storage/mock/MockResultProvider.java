package com.arextest.storage.mock;

import com.arextest.storage.model.enums.MockCategoryType;
import com.arextest.storage.model.mocker.MockItem;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author jmo
 * @since 2021/11/9
 */
public interface MockResultProvider {
    <T extends MockItem> boolean putRecordResult(MockCategoryType category, String recordId, Iterable<T> values);

    <T extends MockItem> boolean putReplayResult(MockCategoryType category, String replayResultId, T value);

    byte[] getRecordResult(MockCategoryType category, @NotNull MockItem mockItem,MockResultContext context);

    List<byte[]> getRecordResultList(MockCategoryType category, String recordId);

    List<byte[]> getReplayResultList(MockCategoryType category, String replayResultId);

    int replayResultCount(MockCategoryType category, String replayResultId);

    int recordResultCount(MockCategoryType category, String recordId);

    boolean removeRecordResult(MockCategoryType category, String recordId);

    boolean removeReplayResult(MockCategoryType category, String replayResultId);
}