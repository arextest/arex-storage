package com.arextest.storage.repository;

import com.arextest.model.mock.Mocker;
import jakarta.validation.constraints.NotNull;

/**
 * @author jmo
 * @since 2021/11/8
 */
public interface RepositoryProvider<T extends Mocker> extends RepositoryReader<T>,
    RepositoryWriter<T> {

  @NotNull
  String getProviderName();

  @NotNull
  String getMockerType();
}