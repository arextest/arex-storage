package com.arextest.storage.sqlparse.action;

import com.arextest.storage.sqlparse.Parse;
import com.arextest.storage.sqlparse.constants.Constants;
import com.arextest.storage.sqlparse.select.ArexSelectVisitorAdapter;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import org.json.JSONObject;

/**
 * Created by rchen9 on 2023/1/6.
 */
public class SelectParse implements Parse<Select> {
    @Override
    public Object parse(Select parseObj) {
        JSONObject sqlObject = new JSONObject();
        sqlObject.put(Constants.ACTION, Constants.SELECT);
        SelectBody selectBody = parseObj.getSelectBody();
        ArexSelectVisitorAdapter arexSelectVisitorAdapter = new ArexSelectVisitorAdapter(sqlObject);
        selectBody.accept(arexSelectVisitorAdapter);
        return sqlObject;
    }
}
