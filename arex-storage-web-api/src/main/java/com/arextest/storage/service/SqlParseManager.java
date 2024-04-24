package com.arextest.storage.service;

import com.arextest.model.constants.DbParseConstants;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author niyan
 * @date 2024/4/23
 * @since 1.0.0
 */
@Slf4j
@Component
public class SqlParseManager {
    /**
     * parse table and action from sql
     * @param sql sql
     * @return Map<String, String> table and action
     */
    public Map<String, String> parseTableAndAction(String sql) {
        Map<String, String> result = new HashMap<>(2);

        if (StringUtils.isEmpty(sql)) {
            return result;
        }
        try {
            sql = sql.replaceAll("\\s+", " ");

            Statement statement = CCJSqlParserUtil.parse(sql);
            result.put(DbParseConstants.ACTION, statement.getClass().getSimpleName());

            TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
            List<String> tableNames = tablesNamesFinder.getTableList(statement);
            if (tableNames != null && !tableNames.isEmpty()) {
                result.put(DbParseConstants.TABLE, String.join(",", tableNames));
            }
        } catch (Exception e) {
            LOGGER.warn("parse sql error, sql: {}", sql, e);
        }
        return result;
    }
}
