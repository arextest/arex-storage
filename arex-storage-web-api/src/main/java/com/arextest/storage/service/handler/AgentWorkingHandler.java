package com.arextest.storage.service.handler;

import java.util.Collections;
import java.util.List;

public interface AgentWorkingHandler<T> {

  /**
   * Save a single piece of data
   * @param item
   * @return
   */
  default boolean save(T item) {
    return true;
  }

  /**
   * Save multiple pieces of data
   * @param items
   * @return
   */
  boolean batchSave(List<T> items);

  /**
   * Query data through recordId and replayId
   * @param recordId
   * @param replayId
   * @return
   */
  default List<T> findBy(String recordId, String replayId) {
    return Collections.emptyList();
  }

}
