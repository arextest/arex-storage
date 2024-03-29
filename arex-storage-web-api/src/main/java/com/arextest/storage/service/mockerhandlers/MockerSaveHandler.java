package com.arextest.storage.service.mockerhandlers;

import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker;

public interface MockerSaveHandler {

  MockCategoryType getMockCategoryType();

  void handle(Mocker item);
}
