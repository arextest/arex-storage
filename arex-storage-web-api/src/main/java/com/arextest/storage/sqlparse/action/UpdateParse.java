package com.arextest.storage.sqlparse.action;

import com.arextest.storage.sqlparse.Parse;
import com.arextest.storage.sqlparse.constants.Constants;
import com.arextest.storage.sqlparse.select.ArexExpressionVisitorAdapter;
import com.arextest.storage.sqlparse.select.ArexFromItemVisitorAdapter;
import com.arextest.storage.sqlparse.select.ArexOrderByVisitorAdapter;
import com.arextest.storage.sqlparse.select.utils.JoinParseUtil;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.Limit;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.update.UpdateSet;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by rchen9 on 2023/1/6.
 */
public class UpdateParse implements Parse<Update> {
    @Override
    public Object parse(Update parseObj) {
        JSONObject sqlObject = new JSONObject();
        sqlObject.put(Constants.ACTION, Constants.UPDATE);

        // table parse
        Table table = parseObj.getTable();
        if (table != null) {
            sqlObject.put(Constants.TABLE, table.getFullyQualifiedName());
        }

        // startJoins parse
        List<Join> startJoins = parseObj.getStartJoins();
        if (startJoins != null && !startJoins.isEmpty()) {
            JSONArray joinArr = new JSONArray();
            startJoins.forEach(item -> {
                joinArr.put(JoinParseUtil.parse(item));
            });
            sqlObject.put(Constants.START_JOINS, joinArr);
        }

        // from parse
        FromItem fromItem = parseObj.getFromItem();
        if (fromItem != null) {
            JSONObject fromObj = new JSONObject();
            ArexFromItemVisitorAdapter arexFromItemVisitorAdapter = new ArexFromItemVisitorAdapter(fromObj);
            fromItem.accept(arexFromItemVisitorAdapter);
            sqlObject.put(Constants.FROM, fromObj);
        }

        // joins parse
        List<Join> joins = parseObj.getJoins();
        if (joins != null && !joins.isEmpty()) {
            JSONArray joinArr = new JSONArray();
            joins.forEach(item -> {
                joinArr.put(JoinParseUtil.parse(item));
            });
            sqlObject.put(Constants.JOIN, joinArr);
        }

        // updateSet parse
        List<UpdateSet> updateSets = parseObj.getUpdateSets();
        if (updateSets != null && !updateSets.isEmpty()) {
            StringBuilder stringBuilder = new StringBuilder();
            UpdateSet.appendUpdateSetsTo(stringBuilder, updateSets);
            sqlObject.put(Constants.SET, stringBuilder.toString());
        }

        // where parse
        Expression where = parseObj.getWhere();
        if (where != null) {
            JSONObject whereObj = new JSONObject();
            whereObj.put(Constants.AND_OR, new JSONArray());
            whereObj.put(Constants.COLUMNS, new JSONObject());
            where.accept(new ArexExpressionVisitorAdapter(whereObj));
        }

        // order parse
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
        if (limit != null){
            sqlObject.put(Constants.LIMIT, limit.toString());
        }
        return sqlObject;
    }
}
