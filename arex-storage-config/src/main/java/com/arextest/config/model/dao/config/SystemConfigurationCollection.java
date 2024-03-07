package com.arextest.config.model.dao.config;

import com.arextest.config.model.dao.BaseEntity;
import com.arextest.config.model.dto.system.ComparePluginInfo;
import com.arextest.config.model.dto.system.DesensitizationJar;
import java.util.Map;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

/**
 * @author wildeslam.
 * @create 2024/2/21 19:43
 */
@Data
@FieldNameConstants
public class SystemConfigurationCollection extends BaseEntity {

  public static final String DOCUMENT_NAME = "SystemConfiguration";

  /**
   * The problem of prohibiting concurrent repeated insertions, the key is unique the function of
   * this record
   */
  private String key;
  private Map<String, Integer> refreshTaskMark;
  private DesensitizationJar desensitizationJar;
  private String callbackUrl;
  private Boolean authSwitch;
  private ComparePluginInfo comparePluginInfo;


  public interface KeySummary {
    String REFRESH_DATA = "refresh_data";
    String DESERIALIZATION_JAR = "deserialization_jar";
    String CALLBACK_URL = "callback_url";
    String AUTH_SWITCH = "auth_switch";
    String COMPARE_PLUGIN_INFO = "compare_plugin_info";

  }
}
