package com.arextest.storage.service.handler.mocker;

import java.util.Collections;
import java.util.List;

public interface AgentWorkingHandler<T> {

  /**
   * Save multiple pieces of data
   * @param items
   * @return
   */
  boolean batchSave(List<T> items);

  /**
   * Query data through replayId
   * @param replayId
   * @return
   */
  default List<T> findBy(String replayId) {
    return Collections.emptyList();
  }

  /**
   * batch query records
   * @param recordId
   * @param fieldNames
   * @param categoryList
   * @return
   */
  default List<T> batchQuery(String recordId, String[] fieldNames, String[] categoryList) {
    return Collections.emptyList();
  }

}
