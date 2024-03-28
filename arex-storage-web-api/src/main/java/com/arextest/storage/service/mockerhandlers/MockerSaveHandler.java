package com.arextest.storage.service.mockerhandlers;

import com.arextest.model.mock.MockCategoryType;

public interface MockerSaveHandler<T> {

  MockCategoryType getMockCategoryType();

  void handle(T item);
}
