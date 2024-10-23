package com.arextest.storage.service.handler.mocker.database;

import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker;
import com.arextest.storage.service.handler.mocker.MockerHandler;
import com.arextest.storage.service.DatabaseParseService;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;


/**
 * @author niyan
 * @date 2024/4/23
 * @since 1.0.0
 */
@Component
public class DatabaseMockerHandler implements MockerHandler {

    @Resource
    private DatabaseParseService databaseParseService;

    @Override
    public MockCategoryType getMockCategoryType() {
        return MockCategoryType.DATABASE;
    }

    @Override
    public void handleOnRecordSaving(Mocker mocker) {
        databaseParseService.regenerateOperationName(mocker);
    }

    @Override
    public void handleOnRecordMocking(Mocker mocker) {
        databaseParseService.regenerateOperationName(mocker);
    }
}
