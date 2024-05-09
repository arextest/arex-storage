package com.arextest.storage.utils;

import com.arextest.model.constants.MockAttributeNames;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker;
import com.arextest.storage.model.TableSchema;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;

/**
 * @author niyan
 * @date 2024/4/24
 * @since 1.0.0
 */
@Slf4j
public class DatabaseUtils {

    private DatabaseUtils() {
    }

    private static final Pattern PATTERN = Pattern.compile("\\s+");

    public static void regenerateOperationName(Mocker mocker) {
        if (!MockCategoryType.DATABASE.getName().equals(mocker.getCategoryType().getName())) {
            return;
        }
        if (mocker.getTargetRequest() ==  null || StringUtils.isEmpty(mocker.getTargetRequest().getBody())) {
            return;
        }
        // If the operation name already contains -, it means that it has already been generated and does not need to be generated again
        if (StringUtils.contains(mocker.getOperationName(), '-')) {
            return;
        }

        String[] sqls = StringUtils.split(mocker.getTargetRequest().getBody(), ";");
        List<String> operationNames = new ArrayList<>(sqls.length);
        for (String sql : sqls) {
            TableSchema tableSchema = parse(sql);
            if (tableSchema == null) {
                continue;
            }
            tableSchema.setDbName(mocker.getTargetRequest().attributeAsString(MockAttributeNames.DB_NAME));
            operationNames.add(regenerateOperationName(tableSchema, mocker.getOperationName()));
        }
        if (operationNames.isEmpty()) {
            return;
        }
        mocker.setOperationName(StringUtils.join(operationNames, ";"));
    }

    /**
     * The operation name is generated in the format of dbName-tableNames-action-originalOperationName, eg: db1-table1,table2-select-operation1
     */
    private static String regenerateOperationName(TableSchema tableSchema, String originOperationName) {
        return new StringBuilder(100).append(StringUtils.defaultString(tableSchema.getDbName())).append('-')
            .append(StringUtils.defaultString(StringUtils.join(tableSchema.getTableNames(), ","))).append('-')
            .append(StringUtils.defaultString(tableSchema.getAction())).append("-")
            .append(originOperationName)
            .toString();
    }


    public static String parseDbName(String operationName, Mocker.Target targetRequest) {
        String dbName = targetRequest.attributeAsString(MockAttributeNames.DB_NAME);
        if (StringUtils.isNotEmpty(dbName)) {
            return dbName;
        }

        if (StringUtils.isEmpty(operationName)) {
            return operationName;
        }
        int index = operationName.indexOf('-');
        if (index == -1) {
            return operationName;
        }
        return operationName.substring(0, index);
    }

    /**
     * @param operationName example: dbName-tableName, tableName, tableName-action-operationName;
     * @return tableNames
     */
    public static List<String> parseTableNames(String operationName) {
        if (StringUtils.isEmpty(operationName)) {
            return Collections.emptyList();
        }
        int countMatches = StringUtils.countMatches(operationName, "-");
        if (countMatches < 2) {
            return Collections.emptyList();
        }

        String[] operations = StringUtils.split(operationName, ';');
        List<String> tableList = new ArrayList<>(operations.length);
        for (String operation : operations) {
            String[] subOperation = StringUtils.split(operation, '-');
            if (subOperation.length < 1) {
                continue;
            }
            tableList.add(subOperation[1]);
        }
        return tableList;
    }


    /**
     * parse table and action from sql
     * @param sql sql
     * @return Map<String, String> table and action
     */
    private static TableSchema parse(String sql) {
        if (StringUtils.isEmpty(sql)) {
            return null;
        }
        TableSchema tableSchema = new TableSchema();
        try {
            sql = PATTERN.matcher(sql).replaceAll(" ");

            Statement statement = CCJSqlParserUtil.parse(sql);
            tableSchema.setAction(statement.getClass().getSimpleName());

            tableSchema.setTableNames(new TablesNamesFinder().getTableList(statement));
        } catch (Exception e) {
            LOGGER.warn("parse sql error, sql: {}", sql, e);
        }
        return tableSchema;
    }
}
