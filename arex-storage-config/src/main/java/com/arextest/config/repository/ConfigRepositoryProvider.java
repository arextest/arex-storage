package com.arextest.config.repository;

import java.util.List;

/**
 * @author jmo
 * @since 2022/1/25
 */
public interface ConfigRepositoryProvider<T> extends ConfigRepository<T> {

  List<T> list();

  List<T> listBy(String appId);

  boolean update(T configuration);

  boolean remove(T configuration);

  boolean insert(T configuration);

  default boolean insertList(List<T> configurationList) {
    if (configurationList == null || configurationList.isEmpty()) {
      return false;
    }
    for (T configuration : configurationList) {
      this.insert(configuration);
    }
    return true;
  }

  default boolean removeList(List<T> configurationList) {
    if (configurationList == null || configurationList.isEmpty()) {
      return false;
    }
    for (T configuration : configurationList) {
      this.remove(configuration);
    }
    return true;
  }

  default boolean removeByAppId(String appId) {
    return false;
  }

  default long count(String appId) {
    return 0;
  }

  default T queryById(String id) {
    return null;
  }

  default List<T> queryByInterfaceIdAndOperationId(String interfaceId, String operationId) {
    return null;
  }

  default List<T> listBy(String appId, String operationId) {
    return null;
  }

}
