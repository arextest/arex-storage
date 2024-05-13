package com.arextest.storage.service.handler.mocker;

import com.arextest.model.mock.MockCategoryType;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MockerHandlerFactory {
  private final Map<MockCategoryType, List<MockerHandler>> categoryHandlers;
  public MockerHandlerFactory(@Autowired List<MockerHandler> handlers) {
    this.categoryHandlers = handlers
        .stream()
        .collect(Collectors.groupingBy(MockerHandler::getMockCategoryType));
  }

  public List<MockerHandler> getHandlers(MockCategoryType type) {
    return categoryHandlers.getOrDefault(type, Collections.emptyList());
  }
}
