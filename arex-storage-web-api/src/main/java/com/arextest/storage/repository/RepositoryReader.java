package com.arextest.storage.repository;

import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker;
import com.arextest.model.replay.PagedRequestType;

/**
 * @author jmo
 * @since 2021/11/7
 */
public interface RepositoryReader<T extends Mocker> {
    Iterable<T> queryRecordList(MockCategoryType categoryType, String recordId);

    T queryRecord(Mocker requestType);

    Iterable<T> queryByRange(PagedRequestType rangeRequestType);

    Iterable<T> queryRecordListPaging(PagedRequestType requestType, String lastId);

    long countByRange(PagedRequestType rangeRequestType);
}