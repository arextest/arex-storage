package com.arextest.storage.model;

public interface Constants{

  String APP_ID = "appId";

  String REPLAY_SCHEDULE_CONFIG_COLLECTION_NAME = "ReplayScheduleConfig";

  String CONFIG_COMPARISON_ENCRYPTION_COLLECTION_NAME = "ConfigComparisonEncryption";

  String CONFIG_COMPARISON_EXCLUSIONS_COLLECTION_NAME = "ConfigComparisonExclusion";

  String CONFIG_COMPARISON_IGNORE_CATEGORY_COLLECTION_NAME = "ConfigComparisonIgnoreCategory";

  String CONFIG_COMPARISON_LIST_SORT_COLLECTION_NAME = "ConfigComparisonListSort";

  String CONFIG_COMPARISON_REFERENCE_COLLECTION_NAME = "ConfigComparisonReference";

  // region: fieldNames of Config
  String COMPARE_CONFIG_TYPE = "compareConfigType";
  String OPERATION_ID = "operationId";
  String FS_INTERFACE_ID = "fsInterfaceId";
  String DEPENDENCY_ID = "dependencyId";
  String DATA_CHANGE_CREATE_TIME  = "dataChangeCreateTime";
  String DATA_CHANGE_UPDATE_TIME = "dataChangeUpdateTime";
  String EXPIRATION_TYPE = "expirationType";
  String EXPIRATION_TIME = "expirationTime";
  // endregion

  // applicationConfig
  String MAX_SQL_LENGTH = "maxSqlLength";
  int MAX_SQL_LENGTH_DEFAULT = 5000;
  String AREX_CONFIG_MOCKERCONVERT_ENABLED = "arex.config.mockerConvert.enabled";
  boolean AREX_CONFIG_MOCKERCONVERT_ENABLED_DEFAULT = true;
  String SQL_PARSE_DURATION_THRESHOLD = "sql.parse.duration.threshold";
  int SQL_PARSE_DURATION_THRESHOLD_DEFAULT = 200;
  String AGENT_VERSION = "agentVersion";
  long TEN_MINUTES = 10 * 60L;

}
