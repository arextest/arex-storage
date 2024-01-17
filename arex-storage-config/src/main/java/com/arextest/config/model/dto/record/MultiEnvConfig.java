package com.arextest.config.model.dto.record;

import java.util.List;

public interface MultiEnvConfig<T> {
  List<T> getMultiEnvConfigs();
  void setMultiEnvConfigs(List<T> multiEnvConfigs);
}
