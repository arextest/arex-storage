package com.arextest.storage.service.mockerhandlers;

import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("unchecked")
public class MockerHandlerFactory {

  @Resource
  List<MockerSaveHandler<?>> handlers;

  public <T extends Mocker> List<MockerSaveHandler<T>> getHandler(MockCategoryType type) {
    List<MockerSaveHandler<T>> result = new ArrayList<>();
    for (MockerSaveHandler<?> handler : handlers) {
      if (handler.getMockCategoryType().equals(type)) {
        result.add((MockerSaveHandler<T>) handler);
      }
    }
    return result;
  }
}
