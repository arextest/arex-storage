package com.arextest.storage.service.mockerhandlers;

import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker;
import javax.servlet.http.HttpServletRequest;

public interface MockerSaveHandler {

  MockCategoryType getMockCategoryType();

  void handle(Mocker item, String forceRecord);
}
