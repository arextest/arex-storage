package com.arextest.storage.service.mockerhandlers;

import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

@Component
@SuppressWarnings("unchecked")
public class MockerHandlerFactory {
    @Resource
    List<MockerSaveHandler<?>> handlers;

    public <T extends Mocker> MockerSaveHandler<T> getHandler(MockCategoryType type) {
        for (MockerSaveHandler<?> handler : handlers) {
            if (handler.getMockCategoryType().equals(type)) {
                return (MockerSaveHandler<T>) handler;
            }
        }
        return null;
    }
}
