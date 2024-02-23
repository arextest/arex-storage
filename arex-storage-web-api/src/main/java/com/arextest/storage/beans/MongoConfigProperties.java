package com.arextest.storage.beans;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author wildeslam.
 * @create 2024/2/23 14:42
 */
@ConfigurationProperties(prefix = "mongo.config")
@Getter
@Setter
public class MongoConfigProperties {
  private Long maxIdleTime;
  private Long connectTimeout;
  private Long socketTimeout;
  private Long serverSelectionTimeout;
}
