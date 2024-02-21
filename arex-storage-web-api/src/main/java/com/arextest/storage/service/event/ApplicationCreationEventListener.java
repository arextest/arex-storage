package com.arextest.storage.service.event;

import com.arextest.storage.model.Constants;
import com.arextest.storage.model.event.ApplicationCreationEvent;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.annotation.Resource;
import lombok.NonNull;
import org.apache.commons.collections4.CollectionUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

@Service
public class ApplicationCreationEventListener implements
    ApplicationListener<ApplicationCreationEvent> {

  @Value("#{'${arex.config.default.compare.ignoredCategoryTypes}'.split(',')}")
  private List<String> defaultMockCompareIgnoreConfig;

  private static final String IGNORE_CATEGORY_DETAIL = "ignoreCategoryDetail";
  private static final String OPERATION_TYPE = "operationType";

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
      document.put(IGNORE_CATEGORY_DETAIL, new Document(OPERATION_TYPE, category));
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

        UpdateResult updateResult = mongoDatabase.getCollection(
                Constants.CONFIG_COMPARISON_IGNORE_CATEGORY_COLLECTION_NAME)
            .updateOne(
                filter,
                update,
                new UpdateOptions().upsert(true));
        System.out.println(updateResult.getModifiedCount());
      }
    }
  }
}
