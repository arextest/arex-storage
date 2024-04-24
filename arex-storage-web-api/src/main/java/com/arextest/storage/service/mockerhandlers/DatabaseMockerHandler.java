package com.arextest.storage.service.mockerhandlers;

import com.arextest.model.constants.DbParseConstants;
import com.arextest.model.constants.MockAttributeNames;
import com.arextest.model.mock.AREXMocker;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker;
import com.arextest.storage.mock.MockResultProvider;
import com.arextest.storage.repository.ProviderNames;
import com.arextest.storage.service.MockSourceEditionService;
import com.arextest.storage.service.SqlParseManager;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

/**
 * @author niyan
 * @date 2024/4/23
 * @since 1.0.0
 */
@Component
@Slf4j
@AllArgsConstructor
public class DatabaseMockerHandler implements MockerSaveHandler {
    private MockResultProvider mockResultProvider;
    private MockSourceEditionService mockSourceEditionService;
    private SqlParseManager sqlParseManager;


    @Override
    public MockCategoryType getMockCategoryType() {
        return MockCategoryType.DATABASE;
    }

    @Override
    public void handle(Mocker item) {

        AREXMocker mocker = (AREXMocker) parseMocker(item);
        mockResultProvider.calculateEigen(mocker, true);
        mockSourceEditionService.add(ProviderNames.DEFAULT, mocker);
    }


    public <T extends Mocker> T parseMocker(T item) {

        String originOperationName = item.getOperationName();
        Mocker.Target targetRequest = item.getTargetRequest();
        String dbName = "";
        String sqlBody = "";
        if (targetRequest != null) {
            dbName = targetRequest.attributeAsString(MockAttributeNames.DB_NAME);
            sqlBody = targetRequest.getBody();
        }

        if (StringUtils.isBlank(sqlBody)) {
            return item;
        }

        // sqlParse
        String[] splitSql = sqlBody.split(";");
        StringBuilder basicInfo = new StringBuilder();
        Map<String, String> tableAndAction;
        for (String subSql : splitSql) {
            tableAndAction = sqlParseManager.parseTableAndAction(subSql);
            if (Objects.nonNull(tableAndAction)) {
                String action = tableAndAction.getOrDefault(DbParseConstants.ACTION, "");
                String tableName = tableAndAction.getOrDefault(DbParseConstants.TABLE, "");
                basicInfo.append(dbName).append('-').append(tableName).append('-').append(action).append("-").append(originOperationName).append(";");
            }
        }
        if (StringUtils.isNotEmpty(basicInfo)) {
            item.setOperationName(basicInfo.toString());
        }
        return item;
    }
}
