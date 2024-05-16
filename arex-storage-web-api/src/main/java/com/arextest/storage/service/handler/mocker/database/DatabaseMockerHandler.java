package com.arextest.storage.service.handler.mocker.database;

import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker;
import com.arextest.storage.service.handler.mocker.MockerHandler;
import com.arextest.storage.utils.DatabaseUtils;
import org.springframework.stereotype.Component;

/**
 * @author niyan
 * @date 2024/4/23
 * @since 1.0.0
 */
@Component
public class DatabaseMockerHandler implements MockerHandler {
    @Override
    public MockCategoryType getMockCategoryType() {
        return MockCategoryType.DATABASE;
    }

    @Override
    public void handleOnRecordSaving(Mocker mocker) {
        DatabaseUtils.regenerateOperationName(mocker);
    }

    @Override
    public void handleOnRecordMocking(Mocker mocker) {
        DatabaseUtils.regenerateOperationName(mocker);
    }
}
