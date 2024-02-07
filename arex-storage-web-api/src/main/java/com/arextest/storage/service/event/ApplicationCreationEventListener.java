package com.arextest.storage.service.event;

import com.arextest.storage.model.event.ApplicationCreationEvent;
import com.mongodb.client.MongoDatabase;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Resource;
import lombok.NonNull;
import org.apache.commons.collections4.CollectionUtils;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

@Service
public class ApplicationCreationEventListener implements
    ApplicationListener<ApplicationCreationEvent> {

  @Value("#{'${arex.config.default.compare.ignoredCategoryTypes}'.split(',')}")
  private List<String> defaultMockCompareIgnoreConfig;

  @Resource
  private MongoDatabase mongoDatabase;

  @Override
  public void onApplicationEvent(@NonNull ApplicationCreationEvent event) {
    String appId = (String) event.getSource();

    // create default mock compare ignore config
    createDefaultMockCompareIgnoreConfig(appId);
  }


  private void createDefaultMockCompareIgnoreConfig(String appId) {
    if (CollectionUtils.isEmpty(defaultMockCompareIgnoreConfig)) {
      return;
    }

    List<Document> mockCompareIgnoreConfig = new ArrayList<>();
    for (String category : defaultMockCompareIgnoreConfig) {
      if (category == null || category.isEmpty()) {
        continue;
      }
      long currentTimeMillis = System.currentTimeMillis();
      Document document = new Document();
      document.put("appId", appId);
      // to see com.arextest.web.model.contract.contracts.common.enums.CompareConfigType in arex-api
      // it represents the config of comparison, which is main entrance
      document.put("compareConfigType", 0);
      document.put("ignoreCategoryDetail", new Document("operationType", category));
      document.put("dataChangeCreateTime", currentTimeMillis);
      document.put("dataChangeUpdateTime", currentTimeMillis);
      // to see com.arextest.web.model.contract.contracts.common.enums.ExpirationType in arex-api
      // it represents the config pinned forever use it
      document.put("expirationType", 0);
      document.put("expirationTime", currentTimeMillis);
      mockCompareIgnoreConfig.add(document);
    }

    if (CollectionUtils.isNotEmpty(mockCompareIgnoreConfig)) {
      mongoDatabase.getCollection("ConfigComparisonIgnoreCategory").insertMany(mockCompareIgnoreConfig);
    }

  }

}
