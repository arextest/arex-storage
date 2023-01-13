package com.arextest.storage.sqlparse.select;

import com.arextest.storage.sqlparse.constants.Constants;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.OrderByVisitor;
import org.json.JSONObject;

/**
 * Created by rchen9 on 2023/1/11.
 */
public class ArexOrderByVisitorAdapter implements OrderByVisitor {

    private JSONObject sqlObject;

    public ArexOrderByVisitorAdapter(JSONObject object) {
        sqlObject = object;
    }

    @Override
    public void visit(OrderByElement orderBy) {
        sqlObject.put(orderBy.toString(), Constants.EMPTY);
    }
}
