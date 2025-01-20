package com.arextest.storage.model;

import com.arextest.model.mock.AREXMocker;
import com.arextest.model.mock.AREXQueryMocker;

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
  String SQL_PARSE_FAIL_OUTPUT_SWITCH = "sql.parse.fail.output.switch";
  String AGENT_VERSION = "agentVersion";
  String CLAZZ_NAME_AREX_MOCKER = AREXMocker.class.getSimpleName();
  String CLAZZ_NAME_AREX_QUERY_MOCKER = AREXQueryMocker.class.getSimpleName();
  String QUERY_CONFIG_URL = "query.config.url";
  String QUERY_SCHEDULE_REPLAY_CONFIG_URL = "query.schedule.replay.config.url";
  String UPDATE_CASE_STATUS_URL = "update.case.status.url";

}
