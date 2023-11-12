package com.arextest.storage.mock;

import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker;
import java.util.List;
import javax.validation.constraints.NotNull;

/**
 * @author jmo
 * @since 2021/11/9
 */
public interface MockResultProvider {

  <T extends Mocker> boolean putRecordResult(MockCategoryType categoryType, String recordId,
      Iterable<T> values);

  <T extends Mocker> boolean putReplayResult(T value);

  byte[] getRecordResult(@NotNull Mocker mockItem, MockResultContext context);

  List<byte[]> getRecordResultList(MockCategoryType category, String recordId);

  List<byte[]> getReplayResultList(MockCategoryType category, String replayId);

  int replayResultCount(MockCategoryType category, String replayResultId);

  int recordResultCount(MockCategoryType category, String recordId);

  <T extends Mocker> boolean removeRecordResult(MockCategoryType category, String recordId,
      Iterable<T> values);

  boolean removeReplayResult(MockCategoryType category, String replayResultId);
}