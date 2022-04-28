package com.arextest.storage.core.repository;

import com.arextest.storage.model.mocker.MockItem;
import com.arextest.storage.model.enums.MockCategoryType;

import javax.validation.constraints.NotNull;

/**
 * @author jmo
 * @since 2021/11/8
 */
public interface RepositoryProvider<T extends MockItem> extends RepositoryReader<T>, RepositoryWriter<T> {
    @NotNull
    MockCategoryType getCategory();
}
