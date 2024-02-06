package com.arextest.storage.enums;

import com.sun.tools.javac.util.Pair;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

/**
 * @author wildeslam.
 * @create 2024/2/4 20:17
 */
@Getter
@AllArgsConstructor
public enum MongoCollectionIndexConfigEnum {

  // new index config added here
  // collection dimension config

  // storage index config
  APP_INDEX("App",
      Collections.singletonList(
          IndexConfig.builder()
              .fieldConfigs(Collections.singletonList(FieldConfig.build("appId", true)))
              .unique(true)
              .ttlIndexConfig(null)
              .build())),

  RECORD_SERVICE_CONFIG_INDEX("RecordServiceConfig",
      Collections.singletonList(
          IndexConfig.builder()
              .fieldConfigs(Collections.singletonList(FieldConfig.build("appId", true)))
              .unique(true)
              .ttlIndexConfig(null)
              .build())),

  SERVICE_OPERATION_INDEX("ServiceOperation",
      Collections.singletonList(
          IndexConfig.builder()
              .fieldConfigs(Arrays.asList(
                  FieldConfig.build("appId", true),
                  FieldConfig.build("operationId", true),
                  FieldConfig.build("operationName", true))
              )
              .unique(true)
              .ttlIndexConfig(null)
              .build())),

  INSTANCES_INDEX("Instances",
      Collections.singletonList(
          IndexConfig.builder()
              .fieldConfigs(Collections.singletonList(FieldConfig.build("dataUpdateTime", true)))
              .unique(false)
              .ttlIndexConfig(new TtlIndexConfig(65L, TimeUnit.SECONDS))
              .build())),

  // report index config
  REPLAY_SCHEDULE_CONFIG_INDEX("ReplayScheduleConfig",
      Collections.singletonList(
          IndexConfig.builder()
              .fieldConfigs(Collections.singletonList(FieldConfig.build("appId", true)))
              .unique(true)
              .ttlIndexConfig(null)
              .build())),

  LOGS_INDEX("logs",
      Collections.singletonList(
          IndexConfig.builder()
              .fieldConfigs(Collections.singletonList(FieldConfig.build("appId", false)))
              .unique(false)
              .ttlIndexConfig(new TtlIndexConfig(10L, TimeUnit.DAYS))
              .build())),

  APP_CONTRACT_INDEX_1("AppContract",
      Arrays.asList(
          IndexConfig.builder()
              .fieldConfigs(Collections.singletonList(FieldConfig.build("appId", true)))
              .unique(false)
              .ttlIndexConfig(null)
              .build(),
          IndexConfig.builder()
              .fieldConfigs(Collections.singletonList(FieldConfig.build("operationId", true)))
              .unique(false)
              .ttlIndexConfig(null)
              .build(),
          IndexConfig.builder()
              .fieldConfigs(Arrays.asList(
                  FieldConfig.build("appId", true),
                  FieldConfig.build("operationId", true),
                  FieldConfig.build("operationName", true),
                  FieldConfig.build("contractType", true)))
              .unique(true)
              .ttlIndexConfig(null)
              .build()
      )),


  SYSTEM_CONFIGURATION_INDEX("SystemConfiguration",
      Collections.singletonList(
          IndexConfig.builder()
              .fieldConfigs(Collections.singletonList(FieldConfig.build("key", true)))
              .unique(true)
              .ttlIndexConfig(null)
              .build())),

  // schedule index config

  REPLAY_RUN_DETAILS_INDEX("ReplayRunDetails",
      Collections.singletonList(
          IndexConfig.builder()
              .fieldConfigs(Arrays.asList(
                  FieldConfig.build("appId", true),
                  FieldConfig.build("sendStatus", true)))
              .unique(false)
              .ttlIndexConfig(null)
              .build())),

  REPLAY_BIZ_LOG_INDEX("ReplayBizLog",
      Collections.singletonList(
          IndexConfig.builder()
              .fieldConfigs(Collections.singletonList(FieldConfig.build("planId", true)))
              .unique(false)
              .ttlIndexConfig(null)
              .build())),
  ;

  private String collectionName;
  private List<IndexConfig> indexConfigs;

  @Data
  @Builder
  public static class IndexConfig {

    private List<FieldConfig> fieldConfigs;
    private Boolean unique;
    private TtlIndexConfig ttlIndexConfig;
  }

  @Data
  @AllArgsConstructor
  public static class TtlIndexConfig {

    private Long expireAfter;
    private TimeUnit timeUnit;
  }

  @Data
  public static class FieldConfig {

    private String fieldName;
    // default is true
    private Boolean ascending;

    public static final Map<Pair, FieldConfig> FIELD_CONFIGS = new HashMap<>();

    public static FieldConfig build(String fieldName, Boolean ascending) {
      Pair<String, Boolean> pair = Pair.of(fieldName, ascending);
      if (FIELD_CONFIGS.containsKey(pair)) {
        return FIELD_CONFIGS.get(pair);
      } else {
        FieldConfig fieldConfig = new FieldConfig();
        fieldConfig.setFieldName(fieldName);
        fieldConfig.setAscending(ascending);
        FIELD_CONFIGS.put(pair, fieldConfig);
        return fieldConfig;
      }
    }
  }
}
