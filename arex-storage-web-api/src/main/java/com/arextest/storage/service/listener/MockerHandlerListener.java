package com.arextest.storage.service.listener;

import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker;
import com.arextest.storage.mock.MockResultContext;
import com.arextest.storage.service.handler.mocker.MockerHandler;
import com.arextest.storage.service.handler.mocker.MockerHandlerFactory;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

/**
 * @since 2024/5/10
 */
@Slf4j
@Component
@AllArgsConstructor
public class MockerHandlerListener implements AgentWorkingListener {
    private final MockerHandlerFactory mockerHandlerFactory;
    @Override
    public boolean onRecordSaving(Mocker instance) {
        return applyHandle(instance.getCategoryType(), handler -> handler.handleOnRecordSaving(instance));
    }

    @Override
    public boolean onRecordMocking(Mocker instance, MockResultContext context) {
        return applyHandle(instance.getCategoryType(), handler -> handler.handleOnRecordMocking(instance));
    }

    private boolean applyHandle(MockCategoryType category, Consumer<MockerHandler> handlerConsumer) {
        List<MockerHandler> handlers = mockerHandlerFactory.getHandlers(category);
        if (CollectionUtils.isEmpty(handlers)) {
            return false;
        }
        for (MockerHandler handler : handlers) {
            try {
                handlerConsumer.accept(handler);
            } catch (Exception e) {
                LOGGER.error("category: {}, handler: {}, pre handle error", category.getName(), handler.getClass().getSimpleName(), e);
            }
            if (!handler.isContinue()) {
                return true;
            }
        }

        return false;
    }
}
