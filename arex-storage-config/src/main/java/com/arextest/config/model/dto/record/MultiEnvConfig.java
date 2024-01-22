package com.arextest.config.model.dto.record;

import java.util.List;
import java.util.Map;

public interface MultiEnvConfig<T> {
  List<MultiEnvConfig<T>> getMultiEnvConfigs();
  void setMultiEnvConfigs(List<MultiEnvConfig<T>> multiEnvConfigs);

  Map<String, String> getEnvTags();
  void setEnvTags(Map<String, String> envTags);
}
