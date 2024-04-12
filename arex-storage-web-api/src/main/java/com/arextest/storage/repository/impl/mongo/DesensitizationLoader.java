package com.arextest.storage.repository.impl.mongo;

import com.arextest.common.model.classloader.RemoteJarClassLoader;
import com.arextest.common.utils.RemoteJarLoaderUtils;
import com.arextest.config.model.dao.config.SystemConfigurationCollection;
import com.arextest.config.model.dao.config.SystemConfigurationCollection.KeySummary;
import com.arextest.extension.desensitization.DataDesensitization;
import com.arextest.extension.desensitization.DefaultDataDesensitization;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.conversions.Bson;

@Slf4j
public class DesensitizationLoader {
  private static final String SYSTEM_CONFIGURATION = "SystemConfiguration";
  private static final String DESENSITIZATION_JAR = "desensitizationJar";
  private static final String JAR_URL = "jarUrl";

  public static final DataDesensitization DEFAULT_DESENSITIZATION_SERVICE = new DefaultDataDesensitization();
  public static DataDesensitization DESENSITIZATION_SERVICE = DEFAULT_DESENSITIZATION_SERVICE;

  public static DataDesensitization load(MongoDatabase mongoDatabase) {
    String jarUrl = getJarUrl(mongoDatabase);
    return loadDesensitization(jarUrl);
  }

  public static DataDesensitization get() {
    return DESENSITIZATION_SERVICE;
  }

  private static String getJarUrl(MongoDatabase mongoDatabase) {
    MongoCollection<Document> collection = mongoDatabase.getCollection(SYSTEM_CONFIGURATION);
    if (collection.countDocuments() <= 0) {
      return null;
    }
    Bson filter = Filters.eq(SystemConfigurationCollection.Fields.key,
        KeySummary.DESERIALIZATION_JAR);
    Document document = collection.find(filter).first();
    if (document != null && document.get(DESENSITIZATION_JAR) != null) {
      return document.get(DESENSITIZATION_JAR, Document.class).getString(JAR_URL);
    }
    return null;
  }

  private static DataDesensitization loadDesensitization(String remoteJarUrl) {
    DataDesensitization dataDesensitization = new DefaultDataDesensitization();
    if (StringUtils.isEmpty(remoteJarUrl)) {
      return dataDesensitization;
    }
    try {
      RemoteJarClassLoader remoteJarClassLoader = RemoteJarLoaderUtils.loadJar(remoteJarUrl);
      dataDesensitization = RemoteJarLoaderUtils
          .loadService(DataDesensitization.class, remoteJarClassLoader)
          .get(0);
    } catch (Exception e) {
      LOGGER.error("load desensitization error", e);
      throw new RuntimeException(e);
    }
    return dataDesensitization;
  }
}
