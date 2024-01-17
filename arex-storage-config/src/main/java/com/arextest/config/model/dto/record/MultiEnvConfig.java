package com.arextest.config.model.dto.record;

import java.util.List;
import java.util.Map;

public interface MultiEnvConfig<T> {
  List<T> getMultiEnvConfigs();
  void setMultiEnvConfigs(List<T> multiEnvConfigs);

  Map<String, String> getEnvTags();
  void setEnvTags(Map<String, String> envTags);
}
