package com.arextest.storage.sqlparse;

import com.arextest.storage.sqlparse.action.ActionFactory;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import org.json.JSONObject;

/**
 * Created by rchen9 on 2023/1/12.
 */
public class SqlParseUtil {

    private static final String ORIGINAL_SQL = "originalSql";

    public static Object sqlParse(String sql) {
        JSONObject sqlObj = new JSONObject();
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            Parse parse = ActionFactory.selectParse(statement);
            sqlObj = (JSONObject) parse.parse(statement);
        } catch (Throwable throwable) {
            sqlObj.put(ORIGINAL_SQL, sql);
        }
        return sqlObj;
    }


}
