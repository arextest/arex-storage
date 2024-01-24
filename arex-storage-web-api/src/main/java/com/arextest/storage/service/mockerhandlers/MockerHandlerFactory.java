package com.arextest.storage.service.mockerhandlers;

import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MockerHandlerFactory {
  private final Map<MockCategoryType, List<MockerSaveHandler<Mocker>>> categoryHandlers;
  public MockerHandlerFactory(@Autowired List<MockerSaveHandler<Mocker>> handlers) {
    this.categoryHandlers = handlers
        .stream()
        .collect(Collectors.groupingBy(MockerSaveHandler::getMockCategoryType));
  }

  public List<MockerSaveHandler<Mocker>> getHandlers(MockCategoryType type) {
    return categoryHandlers.getOrDefault(type, Collections.emptyList());
  }
}
