package io.arex.storage.core.repository;

import io.arex.storage.model.mocker.MockItem;

import java.util.List;

/**
 * @author jmo
 * @since 2021/11/7
 */
public interface RepositoryWriter<T extends MockItem> {
    boolean save(T objectValue);

    boolean saveList(List<T> objectValueList);
}
