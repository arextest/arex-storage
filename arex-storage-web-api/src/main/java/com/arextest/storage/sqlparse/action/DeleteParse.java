package com.arextest.storage.sqlparse.action;

import com.arextest.storage.sqlparse.Parse;
import com.arextest.storage.sqlparse.constants.Constants;
import com.arextest.storage.sqlparse.select.ArexExpressionVisitorAdapter;
import com.arextest.storage.sqlparse.select.ArexOrderByVisitorAdapter;
import com.arextest.storage.sqlparse.select.utils.JoinParseUtil;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.Limit;
import net.sf.jsqlparser.statement.select.OrderByElement;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by rchen9 on 2023/1/6.
 */
public class DeleteParse implements Parse<Delete> {
    @Override
    public Object parse(Delete parseObj) {
        JSONObject sqlObject = new JSONObject();
        sqlObject.put(Constants.ACTION, Constants.DELETE);

        // tables parse
        List<Table> tables = parseObj.getTables();
        if (tables != null && !tables.isEmpty()) {
            JSONObject delTableObj = new JSONObject();
            tables.forEach(item -> {
                delTableObj.put(item.getFullyQualifiedName(), "");
            });
            sqlObject.put(Constants.DEL_TABLES, delTableObj);
        }

        // table parse
        Table table = parseObj.getTable();
        if (table != null) {
            sqlObject.put(Constants.TABLE, table.getFullyQualifiedName());
        }

        // join parse
        List<Join> joins = parseObj.getJoins();
        if (joins != null && !joins.isEmpty()){
            JSONArray joinArr = new JSONArray();
            joins.forEach(item -> {
                joinArr.put(JoinParseUtil.parse(item));
            });
            sqlObject.put(Constants.JOIN, joinArr);
        }

        // where parse
        Expression where = parseObj.getWhere();
        if (where != null) {
            JSONObject whereObj = new JSONObject();
            whereObj.put(Constants.AND_OR, new JSONArray());
            whereObj.put(Constants.COLUMNS, new JSONObject());
            where.accept(new ArexExpressionVisitorAdapter(whereObj));
            sqlObject.put(Constants.WHERE, whereObj);
        }

        // orderby parse
        List<OrderByElement> orderByElements = parseObj.getOrderByElements();
        if (orderByElements != null && !orderByElements.isEmpty()) {
            JSONObject orderByObj = new JSONObject();
            ArexOrderByVisitorAdapter arexOrderByVisitorAdapter = new ArexOrderByVisitorAdapter(orderByObj);
            orderByElements.forEach(item -> {
                item.accept(arexOrderByVisitorAdapter);
            });
            sqlObject.put(Constants.ORDER_BY, orderByObj);
        }

        // limit parse
        Limit limit = parseObj.getLimit();
        if (limit != null) {
            sqlObject.put(Constants.LIMIT, limit.toString());
        }
        return sqlObject;
    }
}
