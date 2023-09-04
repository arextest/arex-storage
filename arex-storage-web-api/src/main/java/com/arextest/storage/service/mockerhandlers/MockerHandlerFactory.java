package com.arextest.storage.service.mockerhandlers;

import com.arextest.model.mock.MockCategoryType;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

@Component
public class MockerHandlerFactory {
    @Resource
    List<MockerSaveHandler> handlers;

    public MockerSaveHandler getHandler(MockCategoryType type) {
        for (MockerSaveHandler handler : handlers) {
            if (handler.getMockCategoryType().equals(type)) {
                return handler;
            }
        }
        return null;
    }
}
