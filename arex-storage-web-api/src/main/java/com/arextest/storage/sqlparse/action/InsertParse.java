package com.arextest.storage.sqlparse.action;

import com.arextest.storage.sqlparse.Parse;
import com.arextest.storage.sqlparse.constants.Constants;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.insert.Insert;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by rchen9 on 2023/1/6.
 */
public class InsertParse implements Parse<Insert> {
    @Override
    public Object parse(Insert parseObj) {
        JSONObject sqlObject = new JSONObject();
        sqlObject.put(Constants.ACTION, Constants.INSERT);

        // table parse
        Table table = parseObj.getTable();
        if (table != null) {
            sqlObject.put(Constants.TABLE, table.getFullyQualifiedName());
        }

        // columns parse
        List<Column> columns = parseObj.getColumns();
        if (columns != null && !columns.isEmpty()) {
            JSONObject columnsObj = new JSONObject();
            columns.forEach(item -> {
                columnsObj.put(item.toString(), Constants.EMPTY);
            });
            sqlObject.put(Constants.COLUMNS, columnsObj);
        }

        // setColumns parse
        List<Column> setColumns = parseObj.getSetColumns();
        if (setColumns != null && !setColumns.isEmpty()) {
            JSONObject setColumnsObj = new JSONObject();
            setColumns.forEach(item -> {
                setColumnsObj.put(item.toString(), Constants.EMPTY);
            });
            sqlObject.put(Constants.COLUMNS, setColumnsObj);
        }

        return sqlObject;
    }
}
