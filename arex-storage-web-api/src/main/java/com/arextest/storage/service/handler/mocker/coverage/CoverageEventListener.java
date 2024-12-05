package com.arextest.storage.service.handler.mocker.coverage;

import com.arextest.model.mock.Mocker;

/**
 * @author: QizhengMo
 * @date: 2024/9/18 18:58
 */
public interface CoverageEventListener {
  void onBeforeNewCaseRecord(Mocker coverageMocker);
  void onNewCaseRecorded(Mocker coverageMocker);

  boolean handleExistingCaseCondition(Mocker coverageMocker);
  void onExistingCaseRecorded(Mocker coverageMocker);
}
