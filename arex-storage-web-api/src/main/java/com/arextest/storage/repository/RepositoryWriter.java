package com.arextest.storage.repository;

import com.arextest.storage.model.mocker.MockItem;

import java.util.List;

/**
 * @author jmo
 * @since 2021/11/7
 */
public interface RepositoryWriter<T extends MockItem> {
    boolean save(T value);

    boolean saveList(List<T> valueList);

    void removeBy(String recordId);

    boolean update(T value);

}