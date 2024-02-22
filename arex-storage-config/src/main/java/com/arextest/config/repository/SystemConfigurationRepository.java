package com.arextest.config.repository;

import com.arextest.config.model.dto.SystemConfiguration;
import java.util.List;

/**
 * @author wildeslam.
 * @create 2024/2/21 19:57
 */
public interface SystemConfigurationRepository {
  boolean saveConfig(SystemConfiguration systemConfig);

  List<SystemConfiguration> getAllSystemConfigList();

  SystemConfiguration getSystemConfigByKey(String key);

}
