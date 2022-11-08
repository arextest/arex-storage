package com.arextest.storage.repository;

import com.arextest.storage.model.enums.MockCategoryType;
import com.arextest.storage.model.mocker.MockItem;

import javax.validation.constraints.NotNull;

/**
 * @author jmo
 * @since 2021/11/8
 */
public interface RepositoryProvider<T extends MockItem> extends RepositoryReader<T>, RepositoryWriter<T> {

    @NotNull
    MockCategoryType getCategory();

    @NotNull
    String getProviderName();
}