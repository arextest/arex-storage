package com.arextest.storage.service.event;

import com.arextest.storage.model.Constants;
import com.arextest.storage.model.event.ApplicationCreationEvent;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.annotation.Resource;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

@Service
public class ApplicationCreationEventListener implements
    ApplicationListener<ApplicationCreationEvent> {

  @Resource
  private MongoDatabase mongoDatabase;

  @Resource
  private CompareConfiguration compareConfiguration;

  private static final String IGNORE_CATEGORY_DETAIL = "ignoreCategoryDetail";

  @Override
  public void onApplicationEvent(@NonNull ApplicationCreationEvent event) {
    String appId = (String) event.getSource();
    // create default mock compare ignore config
    createDefaultMockCompareIgnoreConfig(appId);
  }


  private void createDefaultMockCompareIgnoreConfig(String appId) {
    List<CategoryDetail> ignoredCategoryTypes = compareConfiguration.getIgnoredCategoryTypes();
    if (CollectionUtils.isEmpty(ignoredCategoryTypes)) {
      return;
    }

    List<Document> mockCompareIgnoreConfig = new ArrayList<>();
    for (CategoryDetail category : ignoredCategoryTypes) {
      if (category == null || category.getOperationType().isEmpty()) {
        continue;
      }
      long currentTimeMillis = System.currentTimeMillis();
      Document document = new Document();
      document.put(Constants.APP_ID, appId);
      // to see com.arextest.web.model.contract.contracts.common.enums.CompareConfigType in arex-api
      // it represents the config of comparison, which is main entrance
      document.put(Constants.COMPARE_CONFIG_TYPE, 0);
      document.put(Constants.OPERATION_ID, null);
      document.put(Constants.DEPENDENCY_ID, null);
      document.put(Constants.FS_INTERFACE_ID, null);
      document.put(Constants.DATA_CHANGE_CREATE_TIME, currentTimeMillis);
      document.put(Constants.DATA_CHANGE_UPDATE_TIME, currentTimeMillis);
      // to see com.arextest.web.model.contract.contracts.common.enums.ExpirationType in arex-api
      // it represents the config pinned forever use it
      document.put(Constants.EXPIRATION_TYPE, 0);
      document.put(Constants.EXPIRATION_TIME, new Date());
      document.put(IGNORE_CATEGORY_DETAIL, category);
      mockCompareIgnoreConfig.add(document);
    }

    if (CollectionUtils.isNotEmpty(mockCompareIgnoreConfig)) {
      for (Document document : mockCompareIgnoreConfig) {
        Bson filter = Filters.and(
            Filters.eq(Constants.APP_ID, document.get(Constants.APP_ID)),
            Filters.eq(Constants.COMPARE_CONFIG_TYPE, document.get(Constants.COMPARE_CONFIG_TYPE)),
            Filters.eq(Constants.OPERATION_ID, document.get(Constants.OPERATION_ID)),
            Filters.eq(Constants.DEPENDENCY_ID, document.get(Constants.DEPENDENCY_ID)),
            Filters.eq(Constants.FS_INTERFACE_ID, document.get(Constants.FS_INTERFACE_ID)),
            Filters.eq(IGNORE_CATEGORY_DETAIL, document.get(IGNORE_CATEGORY_DETAIL))
        );

        Bson update = Updates.combine(
            Updates.set(Constants.APP_ID, document.get(Constants.APP_ID)),
            Updates.set(Constants.COMPARE_CONFIG_TYPE, document.get(Constants.COMPARE_CONFIG_TYPE)),
            Updates.set(Constants.OPERATION_ID, document.get(Constants.OPERATION_ID)),
            Updates.set(Constants.DEPENDENCY_ID, document.get(Constants.DEPENDENCY_ID)),
            Updates.set(Constants.FS_INTERFACE_ID, document.get(Constants.FS_INTERFACE_ID)),
            Updates.set(IGNORE_CATEGORY_DETAIL, document.get(IGNORE_CATEGORY_DETAIL)),
            Updates.set(Constants.DATA_CHANGE_CREATE_TIME,
                document.get(Constants.DATA_CHANGE_CREATE_TIME)),
            Updates.set(Constants.DATA_CHANGE_UPDATE_TIME,
                document.get(Constants.DATA_CHANGE_UPDATE_TIME)),
            Updates.set(Constants.EXPIRATION_TYPE, document.get(Constants.EXPIRATION_TYPE)),
            Updates.set(Constants.EXPIRATION_TIME, document.get(Constants.EXPIRATION_TIME))
        );

        mongoDatabase.getCollection(Constants.CONFIG_COMPARISON_IGNORE_CATEGORY_COLLECTION_NAME)
            .updateOne(filter, update, new UpdateOptions().upsert(true));
      }
    }
  }

  @Configuration
  @ConfigurationProperties(prefix = "arex.config.default.compare")
  @Getter
  @Setter
  public static class CompareConfiguration {

    private List<CategoryDetail> ignoredCategoryTypes;

  }

  @Getter
  @Setter
  public static class CategoryDetail {

    private String operationType;
    private String operationName;
  }


}
