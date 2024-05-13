package com.arextest.storage.service.handler.mocker;

import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker;

public interface MockerHandler {

  MockCategoryType getMockCategoryType();

  default void handleOnRecordMocking(Mocker mocker) { }

  default void handleOnRecordSaving(Mocker mocker) { }

  default boolean isContinue() {
    return true;
  }
}
