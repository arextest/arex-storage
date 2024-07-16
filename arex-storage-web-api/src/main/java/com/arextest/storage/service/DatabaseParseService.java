package com.arextest.storage.service;

import com.arextest.common.config.DefaultApplicationConfig;
import com.arextest.model.constants.MockAttributeNames;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker;
import com.arextest.storage.metric.MetricListener;
import com.arextest.storage.model.TableSchema;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.arextest.storage.trace.MDCTracer;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collections;

import static com.arextest.storage.model.Constants.MAX_SQL_LENGTH;
import static com.arextest.storage.model.Constants.MAX_SQL_LENGTH_DEFAULT;
import static com.arextest.storage.model.Constants.SQL_PARSE_DURATION_THRESHOLD;
import static com.arextest.storage.model.Constants.SQL_PARSE_DURATION_THRESHOLD_DEFAULT;

/**
 * @author niyan
 * @date 2024/4/24
 * @since 1.0.0
 */
@Slf4j
@Component
public class DatabaseParseService {

    private static final String SQL_PARSE_TIME_METRIC_NAME = "sql.parse.time";
    private static final Pattern PATTERN = Pattern.compile("(\\s+|\"\\?\"|\\[|\\])");
    private static final String CLIENT_APP_ID = "clientAppId";

    @Autowired(required = false)
    private List<MetricListener> metricListenerList;
    @Resource
    private DefaultApplicationConfig defaultApplicationConfig;


    public void regenerateOperationName(Mocker mocker) {
        if (!MockCategoryType.DATABASE.getName().equals(mocker.getCategoryType().getName())) {
            return;
        }
        if (mocker.getTargetRequest() ==  null || StringUtils.isEmpty(mocker.getTargetRequest().getBody())) {
            return;
        }
        // If the operation name already contains @, it means that it has already been generated and does not need to be generated again
        if (StringUtils.contains(mocker.getOperationName(), '@')) {
            return;
        }
        int maxSqlLengthInt = defaultApplicationConfig.getConfigAsInt(MAX_SQL_LENGTH, MAX_SQL_LENGTH_DEFAULT);

        if (maxSqlLengthInt <= 0) {
            maxSqlLengthInt = MAX_SQL_LENGTH_DEFAULT;
        }

        MDCTracer.addTrace(mocker);

        String[] sqls = StringUtils.split(mocker.getTargetRequest().getBody(), ";");
        List<String> operationNames = new ArrayList<>(sqls.length);
        for (String sql : sqls) {
            if (StringUtils.isEmpty(sql) || StringUtils.startsWithIgnoreCase(sql, "exec sp")) {
                continue;
            }
            if (sql.length() > maxSqlLengthInt) {
                LOGGER.warn("[[title=sqlParse]]skip sql parse cause sql length > config max length {}, sql: {}", maxSqlLengthInt, sql);
                continue;
            }
            TableSchema tableSchema = parse(sql, mocker.getAppId());
            tableSchema.setDbName(mocker.getTargetRequest().attributeAsString(MockAttributeNames.DB_NAME));
            operationNames.add(regenerateOperationName(tableSchema, mocker.getOperationName()));
        }
        if (operationNames.isEmpty()) {
            return;
        }
        mocker.setOperationName(StringUtils.join(operationNames, ";"));

        MDCTracer.clear();
    }

    /**
     * The operation name is generated in the format of dbName-tableNames-action-originalOperationName, eg: db1@table1,table2@select@operation1
     */
    @VisibleForTesting
    public String regenerateOperationName(TableSchema tableSchema, String originOperationName) {
        return new StringBuilder(100).append(StringUtils.defaultString(tableSchema.getDbName())).append('@')
            .append(StringUtils.defaultString(StringUtils.join(tableSchema.getTableNames(), ","))).append('@')
            .append(StringUtils.defaultString(tableSchema.getAction())).append("@")
            .append(originOperationName)
            .toString();
    }


    public String parseDbName(String operationName, Mocker.Target targetRequest) {
        String dbName = targetRequest.attributeAsString(MockAttributeNames.DB_NAME);
        if (StringUtils.isNotEmpty(dbName)) {
            return dbName;
        }

        if (StringUtils.isEmpty(operationName)) {
            return operationName;
        }
        int index = operationName.indexOf('@');
        if (index == -1) {
            return operationName;
        }
        return operationName.substring(0, index);
    }

    /**
     * @param operationName eg: db1@table1,table2@select@operation1;db2@table3,table4@select@operation2;
     * @return tableNames eg: ["table1,table2", "table3,table4"]
     */
    public List<String> parseTableNames(String operationName) {
        if (StringUtils.isEmpty(operationName)) {
            return Collections.emptyList();
        }
        int countMatches = StringUtils.countMatches(operationName, "@");
        if (countMatches < 2) {
            return Collections.emptyList();
        }

        String[] operations = StringUtils.split(operationName, ';');
        List<String> tableList = new ArrayList<>(operations.length);
        for (String operation : operations) {
            String[] subOperation = StringUtils.splitPreserveAllTokens(operation, '@');
            if (subOperation.length < 2 || StringUtils.isEmpty(subOperation[1])) {
                continue;
            }
            tableList.add(subOperation[1]);
        }
        return tableList;
    }

    /**
     * parse table and action from sql
     * @param sql sql
     * @return table schema info
     */
    public TableSchema parse(String sql, String appId) {
        long startTime = System.currentTimeMillis();

        TableSchema tableSchema = new TableSchema();
        try {
            sql = PATTERN.matcher(sql).replaceAll(" ");

            Statement statement = CCJSqlParserUtil.parse(sql);
            tableSchema.setAction(getAction(statement));

            List<String> tableNameList = new TablesNamesFinder().getTableList(statement);
            // sort table name
            if (CollectionUtils.isNotEmpty(tableNameList)) {
                Collections.sort(tableNameList);
            }
            tableSchema.setTableNames(tableNameList);
        } catch (Throwable e) {
            return tableSchema;
        } finally {
            long totalTime = System.currentTimeMillis() - startTime;
            recordParseTime(totalTime, sql, appId);
        }
        return tableSchema;
    }

    private String getAction(Statement statement) {
        if (statement instanceof Select) {
            return "Select";
        }
        return statement.getClass().getSimpleName();
    }

    private void recordParseTime(long duration, String sql, String appId) {
        if (CollectionUtils.isEmpty(metricListenerList)) {
            return;
        }
        int parseTimeThreshold = defaultApplicationConfig.getConfigAsInt(SQL_PARSE_DURATION_THRESHOLD, SQL_PARSE_DURATION_THRESHOLD_DEFAULT);
        if (duration > parseTimeThreshold) {
            LOGGER.warn("[[title=sqlParse]]the actual parsing time:{} exceeds the set threshold:{}, sql: {}", duration, parseTimeThreshold, sql);
        }

        Map<String, String> tags = Collections.singletonMap(CLIENT_APP_ID, appId);
        for (MetricListener metricListener : metricListenerList) {
            metricListener.recordTime(SQL_PARSE_TIME_METRIC_NAME, tags, duration);
        }
    }
}
