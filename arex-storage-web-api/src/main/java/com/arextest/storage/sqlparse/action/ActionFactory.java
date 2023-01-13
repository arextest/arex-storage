package com.arextest.storage.sqlparse.action;
import com.arextest.storage.sqlparse.Parse;
import net.sf.jsqlparser.statement.Statement;

/**
 * Created by rchen9 on 2023/1/6.
 */
public class ActionFactory {
    public static Parse selectParse(Statement statement) {
        String simpleName = statement.getClass().getSimpleName();
        switch (simpleName) {
            case "Select":
                return new SelectParse();
            case "Insert":
                return new InsertParse();
            case "Delete":
                return new DeleteParse();
            case "Update":
                return new UpdateParse();
            default:
                throw new UnsupportedOperationException("not support");
        }
    }
}
