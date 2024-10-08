package com.arextest.storage.service.handler.mocker.coverage;

import com.arextest.model.mock.Mocker;

/**
 * @author: QizhengMo
 * @date: 2024/9/18 19:00
 */
public class DefaultCoverageEventListener implements CoverageEventListener {

  @Override
  public void onBeforeNewCaseRecord(Mocker coverageMocker) {
  }

  @Override
  public void onNewCaseRecorded(Mocker coverageMocker) {
  }

  @Override
  public void onBeforeExistingCaseRecord(Mocker coverageMocker) {
  }

  @Override
  public void onExistingCaseRecorded(Mocker coverageMocker) {
  }
}
