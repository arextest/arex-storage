package com.arextest.config.model.dto;

import java.util.List;
import java.util.Map;

public interface MultiEnvConfig<T> {
  List<T> getMultiEnvConfigs();
  void setMultiEnvConfigs(List<T> multiEnvConfigs);

  Map<String, List<String>> getEnvTags();
  void setEnvTags(Map<String, List<String>> envTags);
}
