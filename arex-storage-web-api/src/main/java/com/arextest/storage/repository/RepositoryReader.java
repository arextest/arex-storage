package com.arextest.storage.repository;

import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker;
import com.arextest.model.replay.PagedRequestType;
import java.util.Map;

/**
 * @author jmo
 * @since 2021/11/7
 */
public interface RepositoryReader<T extends Mocker> {

  Iterable<T> queryRecordList(MockCategoryType categoryType, String recordId);

  Iterable<T> queryRecordList(MockCategoryType categoryType, String recordId, String[] fieldNames);

  T queryRecord(Mocker requestType);

  T queryById(MockCategoryType categoryType, String id);

  Iterable<T> queryEntryPointByRange(PagedRequestType rangeRequestType);

  long countByRange(PagedRequestType rangeRequestType);

  Map<String, Long> countByOperationName(PagedRequestType rangeRequestType);
}