package com.arextest.storage.service.handler;

import java.util.List;

public interface AgentWorkingHandler<T> {

  boolean batchSave(List<T> items);

  List<T> findBy(String recordId, String replayId);

}
