package com.arextest.storage.repository;

import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker;

import java.util.List;

/**
 * @author jmo
 * @since 2021/11/7
 */
public interface RepositoryWriter<T extends Mocker> {
    boolean save(T value);

    boolean saveList(List<T> valueList);

    long removeBy(MockCategoryType categoryType, String recordId);

    boolean update(T value);
}