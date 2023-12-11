package com.arextest.storage.beans;

import com.arextest.model.mock.MockCategoryType;
import com.arextest.storage.model.RecordEnvType;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "arex.storage")
@Getter
@Setter
public class StorageConfigurationProperties {

  private String mongodbUri;
  private Cache cache;
  private Set<MockCategoryType> categoryTypes;
  // @Value("${arex.storage.enable-auto-discovery-entry-point:true}")
  private boolean enableAutoDiscoveryEntryPoint = true;

  private RecordEnvType recordEnv;
  private Map<String, Long> expirationDurationMap;
  private Long defaultExpirationDuration;
  private int allowReRunDays;
  private List<String> supportTags;

  @Getter
  @Setter
  static class Cache {

    private String uri;
    private long expiredSeconds = 7200;
  }

}