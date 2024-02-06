package com.arextest.model.enums;

import com.sun.tools.javac.util.Pair;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

/**
 * @author wildeslam.
 * @create 2024/2/4 20:17
 */
@Getter
@AllArgsConstructor
public enum MongoIndexConfigEnum {

  // new index config added here

  // storage index config
  APP_INDEX("App",
      Collections.singletonList(FieldConfig.build("appId", true)),
      true,
      null),

  RECORD_SERVICE_CONFIG_INDEX("RecordServiceConfig",
      Collections.singletonList(FieldConfig.build("appId", true)),
      true,
      null),

  SERVICE_OPERATION_INDEX("ServiceOperation",
      Arrays.asList(
          FieldConfig.build("appId", true),
          FieldConfig.build("operationId", true),
          FieldConfig.build("operationName", true)),
      true,
      null),

  INSTANCES_INDEX("Instances",
      Collections.singletonList(FieldConfig.build("dataUpdateTime", true)),
      false,
      new TtlIndexConfig(65L, TimeUnit.SECONDS)),

  // report index config
  REPLAY_SCHEDULE_CONFIG_INDEX("ReplayScheduleConfig",
      Collections.singletonList(FieldConfig.build("appId", true)),
      true,
      null),

  LOGS_INDEX("logs",
      Collections.singletonList(FieldConfig.build("date", false)),
      false,
      new TtlIndexConfig(10L, TimeUnit.DAYS)),

  APP_CONTRACT_INDEX_1("AppContract",
      Collections.singletonList(FieldConfig.build("appId", true)),
      false,
      null),

  APP_CONTRACT_INDEX_2("AppContract",
      Collections.singletonList(FieldConfig.build("operationId", true)),
      false,
      null),

  APP_CONTRACT_INDEX_3("AppContract",
      Arrays.asList(
          FieldConfig.build("appId", true),
          FieldConfig.build("operationId", true),
          FieldConfig.build("operationName", true),
          FieldConfig.build("contractType", true))
      ,
      true,
      null),

  SYSTEM_CONFIGURATION_INDEX("SystemConfiguration",
      Collections.singletonList(FieldConfig.build("key", true)),
      true,
      null),

  // schedule index config

  REPLAY_RUN_DETAILS_INDEX("ReplayRunDetails",
      Arrays.asList(
          FieldConfig.build("appId", true),
          FieldConfig.build("sendStatus", true)),
      false,
      null),

  REPLAY_BIZ_LOG_INDEX("ReplayBizLog",
      Collections.singletonList(FieldConfig.build("planId", true)),
      false,
      null),
  ;

  private String collectionName;
  private List<FieldConfig> fieldConfigs;
  private Boolean unique;
  private TtlIndexConfig ttlIndexConfig;


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


  public static final Set<MongoIndexConfigEnum> INDEX_CONFIGS;

  // new index config added here
  static {
    INDEX_CONFIGS = new HashSet<>();
    INDEX_CONFIGS.addAll(Arrays.asList(MongoIndexConfigEnum.values()));

  }

}
