package com.arextest.storage.core.bean;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "arex.storage.mongo")
@Getter
@Setter
public class StorageMongoProperties {
    private String host;
    private String dbName;
}