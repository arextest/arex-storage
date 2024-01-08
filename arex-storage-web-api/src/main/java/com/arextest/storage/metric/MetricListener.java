package com.arextest.storage.metric;


import java.util.Map;

/**
 * created by xinyuan_wang on 2023/5/06
 */
public interface MetricListener {

  /**
   * record request time with tags.
   */
  void recordTime(String metricName, Map<String, String> tags, long timeMillis);

  /**
   * record match strategy count with tags.
   */
  void recordMatchingCount(String metricName, Map<String, String> tags);

  /**
   * record size with tags.
   */
  void recordSize(String metricName, Map<String, String> tags, int size);
}
