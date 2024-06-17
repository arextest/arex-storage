package com.arextest.storage.service.handler.mocker.database;

import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker;
import com.arextest.storage.service.config.ApplicationDefaultConfig;
import com.arextest.storage.service.handler.mocker.MockerHandler;
import com.arextest.storage.utils.DatabaseUtils;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.arextest.storage.model.Constants.MAX_SQL_LENGTH;
import static com.arextest.storage.model.Constants.MAX_SQL_LENGTH_DEFAULT;

/**
 * @author niyan
 * @date 2024/4/23
 * @since 1.0.0
 */
@Component
public class DatabaseMockerHandler implements MockerHandler {

    @Setter(onMethod_={@Autowired})
    private ApplicationDefaultConfig applicationDefaultConfig;


    @Override
    public MockCategoryType getMockCategoryType() {
        return MockCategoryType.DATABASE;
    }

    @Override
    public void handleOnRecordSaving(Mocker mocker) {
        DatabaseUtils.regenerateOperationName(mocker, applicationDefaultConfig.getConfigAsInt(MAX_SQL_LENGTH, MAX_SQL_LENGTH_DEFAULT));
    }

    @Override
    public void handleOnRecordMocking(Mocker mocker) {
        DatabaseUtils.regenerateOperationName(mocker, applicationDefaultConfig.getConfigAsInt(MAX_SQL_LENGTH, MAX_SQL_LENGTH_DEFAULT));
    }
}
