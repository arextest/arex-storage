package com.arextest.storage.repository;

import com.arextest.model.mock.Mocker;

import javax.validation.constraints.NotNull;

/**
 * @author jmo
 * @since 2021/11/8
 */
public interface RepositoryProvider<T extends Mocker> extends RepositoryReader<T>, RepositoryWriter<T> {

    @NotNull
    String getProviderName();
}