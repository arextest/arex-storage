package com.arextest.storage.service.handler.mocker;

import java.util.List;

public interface AgentWorkingHandler<T> {

  /**
   * Save a single piece of data
   * @param item
   * @return
   */
  boolean save(T item);

  /**
   * Save multiple pieces of data
   * @param items
   * @return
   */
  boolean batchSave(List<T> items);

}
