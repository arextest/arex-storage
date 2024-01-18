package com.arextest.config.repository;

import com.arextest.config.model.dto.AbstractMultiEnvConfiguration;

public interface MultiEnvConfigRepositoryProvider<T extends AbstractMultiEnvConfiguration<?>>
    extends ConfigRepositoryProvider<T> {
  boolean updateMultiEnvConfig(T configuration);
}
