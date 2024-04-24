package com.arextest.storage.utils;

import com.arextest.model.constants.MockAttributeNames;
import com.arextest.model.mock.Mocker;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author niyan
 * @date 2024/4/24
 * @since 1.0.0
 */
public class DatabaseUtils {

    private DatabaseUtils() {
    }

    /**
     * The operationName field of the new data is composed of: dbName-tableName, tableName, tableName-action-operationName; the operationName field of the old data is composed of: operationName
     * It is possible that dbName is empty, tableName is empty, and action is empty, and the returned data will be ---operationName
     */
    public static String parseOperationName(String operationName) {
        if (StringUtils.isEmpty(operationName)) {
            return operationName;
        }
        String[] split = operationName.split(";");
        for (String subOperationName : split) {
            String[] baseInfos = subOperationName.split("-");
            if (baseInfos.length > 0) {
                return baseInfos[baseInfos.length - 1];
            }
        }
        return operationName;
    }

    /**
     * @param operationName example: dbName-tableName, tableName, tableName-action-operationName;
     * @return dbName
     */
    public static String parseDbName(String operationName) {
        if (StringUtils.isEmpty(operationName)) {
            return operationName;
        }
        String[] split = operationName.split(";");
        for (String subOperationName : split) {
            String[] baseInfos = subOperationName.split("-");
            if (baseInfos.length == 4) {
                return baseInfos[0];
            }
        }
        return StringUtils.EMPTY;
    }

    public static String parseDbName(Mocker.Target targetRequest) {
        if (targetRequest == null) {
            return StringUtils.EMPTY;
        }
        return targetRequest.attributeAsString(MockAttributeNames.DB_NAME);
    }

    public static String parseDbName(String operationName, Mocker.Target targetRequest) {
        String dbName = parseDbName(operationName);
        if (StringUtils.isNotEmpty(dbName)) {
            return dbName;
        }
        return parseDbName(targetRequest);
    }

    /**
     * @param operationName example: dbName-tableName, tableName, tableName-action-operationName;
     * @return tableNames
     */
    public static Set<String> parseTableNames(String operationName) {
        if (StringUtils.isEmpty(operationName)) {
            return Collections.emptySet();
        }
        Set<String> tableSet = new HashSet<>();
        String[] operations = operationName.split(";");
        for (String operation : operations) {
            String[] parts = operation.split("-");
            if (parts.length == 4) {
                String tableNames = parts[1];
                if (StringUtils.isEmpty(tableNames)) {
                    continue;
                }
                String[] tables = tableNames.split(",");
                Collections.addAll(tableSet, tables);
            }
        }
        return tableSet;
    }
}
