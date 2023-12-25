package com.arextest.storage.repository;

import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker;
import java.util.Date;
import java.util.List;

/**
 * @author jmo
 * @since 2021/11/7
 */
public interface RepositoryWriter<T extends Mocker> {

  boolean save(T value);

  boolean saveList(List<T> valueList);

  long removeBy(MockCategoryType categoryType, String recordId);

  /**
   * To extend all mockers of the given category type and record id to the given expire time.
   * @param categoryType the category type of the mockers to be extended
   * @param recordId the record id of the mockers to be extended
   * @param expireTime the expiration time to be extended to
   * @return true if the operation is successful, false otherwise
   */
  boolean extendExpirationTo(MockCategoryType categoryType, String recordId, Date expireTime);

  boolean update(T value);

  long removeByAppId(MockCategoryType categoryType, String appId);

  long removeByOperationNameAndAppId(MockCategoryType categoryType, String operationName,
      String appId);

}