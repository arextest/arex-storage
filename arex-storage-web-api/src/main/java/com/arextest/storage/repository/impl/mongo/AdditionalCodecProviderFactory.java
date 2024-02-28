package com.arextest.storage.repository.impl.mongo;

import com.arextest.common.model.classloader.RemoteJarClassLoader;
import com.arextest.common.utils.RemoteJarLoaderUtils;
import com.arextest.config.model.dao.config.SystemConfigurationCollection;
import com.arextest.config.model.dao.config.SystemConfigurationCollection.KeySummary;
import com.arextest.extension.desensitization.DataDesensitization;
import com.arextest.extension.desensitization.DefaultDataDesensitization;
import com.arextest.model.mock.Mocker;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import java.util.Collections;
import java.util.List;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.conversions.Bson;

@Data
@Slf4j
public class AdditionalCodecProviderFactory {

  private static final String SYSTEM_CONFIGURATION = "SystemConfiguration";
  private static final String DESENSITIZATION_JAR = "desensitizationJar";
  private static final String JAR_URL = "jarUrl";

  private MongoDatabase mongoDatabase;

  public List<CodecProvider> get() {
    if (mongoDatabase == null) {
      return Collections.emptyList();
    }
    return Collections.singletonList(this.buildAREXMockerCodecProvider(mongoDatabase));
  }

  private AREXMockerCodecProvider buildAREXMockerCodecProvider(MongoDatabase mongoDatabase) {
    String jarUrl = this.getJarUrl(mongoDatabase);
    DataDesensitization dataDesensitization = this.loadDesensitization(jarUrl);

    CompressionCodecImpl<Mocker.Target> targetCompressionCodec =
        new CompressionCodecImpl<>(Mocker.Target.class, dataDesensitization);

    return AREXMockerCodecProvider.builder().targetCodec(targetCompressionCodec).build();
  }

  private String getJarUrl(MongoDatabase mongoDatabase) {
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

  private DataDesensitization loadDesensitization(String remoteJarUrl) {
    DataDesensitization dataDesensitization = new DefaultDataDesensitization();
    if (StringUtils.isEmpty(remoteJarUrl)) {
      return dataDesensitization;
    }

    try {
      RemoteJarClassLoader remoteJarClassLoader = RemoteJarLoaderUtils.loadJar(remoteJarUrl);
      List<DataDesensitization> dataDesensitizations =
          RemoteJarLoaderUtils.loadService(DataDesensitization.class, remoteJarClassLoader);
      dataDesensitization = dataDesensitizations.get(0);
    } catch (Exception e) {
      LOGGER.error("load desensitization error", e);
      throw new RuntimeException(e);
    }
    return dataDesensitization;
  }

}
