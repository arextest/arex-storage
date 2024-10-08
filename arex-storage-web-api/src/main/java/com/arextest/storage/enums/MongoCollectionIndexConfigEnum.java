package com.arextest.storage.enums;

import com.google.common.collect.Lists;
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
import org.apache.commons.lang3.tuple.Pair;

/**
 * @author wildeslam.
 * @create 2024/2/4 20:17
 */
@Getter
@AllArgsConstructor
public enum MongoCollectionIndexConfigEnum {

  // new index config added here
  // collection dimension config

  // region storage index config
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
                  FieldConfig.build("serviceId", true),
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
              .ttlIndexConfig(new TtlIndexConfig(66L, TimeUnit.SECONDS))
              .build())),
  // endregion

  // region report index config
  REPLAY_SCHEDULE_CONFIG_INDEX("ReplayScheduleConfig",
      Collections.singletonList(
          IndexConfig.builder()
              .fieldConfigs(Collections.singletonList(FieldConfig.build("appId", true)))
              .unique(true)
              .ttlIndexConfig(null)
              .build())),

  REPLAY_COMPARE_RESULT_INDEX("ReplayCompareResult",
      Lists.newArrayList(
          IndexConfig.builder()
              .fieldConfigs(Collections.singletonList(FieldConfig.build("planItemId", true)))
              .unique(false)
              .ttlIndexConfig(null)
              .build(),
          IndexConfig.builder()
              .fieldConfigs(Lists.newArrayList(FieldConfig.build("operationId", true),
                  FieldConfig.build("categoryName", true),
                  FieldConfig.build("dataChangeCreateTime", false)))
              .unique(false)
              .ttlIndexConfig(null)
              .build())),

  LOGS_INDEX("logs",
      Arrays.asList(
          IndexConfig.builder()
              .fieldConfigs(Collections.singletonList(FieldConfig.build("date", false)))
              .unique(false)
              .ttlIndexConfig(new TtlIndexConfig(10L, TimeUnit.DAYS))
              .build(),
          IndexConfig.builder()
              .fieldConfigs(Arrays.asList(
                  FieldConfig.build("level", true),
                  FieldConfig.build("millis", true),
                  FieldConfig.build("contextMap.app-type", true))
              )
              .build()
      )),

  APP_CONTRACT_INDEX("AppContract",
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
  // endregion

  // region schedule index config
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
  // endregion

  private String collectionName;
  // indexes at same collection
  private List<IndexConfig> indexConfigs;

  @Data
  @Builder
  public static class IndexConfig {

    // index fields, if not compound index, only one field
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
