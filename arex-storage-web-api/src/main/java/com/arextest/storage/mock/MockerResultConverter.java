package com.arextest.storage.mock;

import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker;

public interface MockerResultConverter {

  <T extends Mocker> T convert(MockCategoryType category, T mocker);

}
