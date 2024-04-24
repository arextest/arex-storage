package com.arextest.storage.service.config.impl;

import com.arextest.model.constants.DbParseConstants;
import com.arextest.storage.service.SqlParseManager;
import net.sf.jsqlparser.JSQLParserException;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * @author niyan
 * @date 2024/4/23
 * @since 1.0.0
 */
public class SqlParseManagerTest {

    private final SqlParseManager sqlParseManager = new SqlParseManager();

    @Test
    public void testSimpleSelect() throws JSQLParserException {
        String sql = "SELECT * FROM mytable";
        Map<String, String> stringStringMap = sqlParseManager.parseTableAndAction(sql);
        assertEquals(DbParseConstants.SELECT, stringStringMap.get("action"));
        assertEquals("mytable", stringStringMap.get("table"));
    }

    @Test
    public void testSelect() throws JSQLParserException {
        String sql = "select b.Name as Department,a.Name as Employee,a.Salary\n" +
                "from (select *,dense_rank() over(partition by departmentid order by Salary desc) as rnk from Employee) a \n"
                +
                "left join department b \n" +
                "on a.departmentid = b.Id and a.aa = b.aa and a.cc = b.cc\n" +
                "where a.rnk <= 3 and a.per_id in (select per_id from colle_subject);\n" +
                "\n";
        Map<String, String> stringStringMap = sqlParseManager.parseTableAndAction(sql);
        assertEquals(DbParseConstants.SELECT, stringStringMap.get("action"));
        assertEquals("Employee,department,colle_subject", stringStringMap.get("table"));
    }

    @Test
    public void testMultiSelect() throws JSQLParserException {
        String sql = "SELECT * FROM students WHERE score = 18; select * from student where score = 20;";
        String[] split = sql.split(";");
        StringBuilder operationName = new StringBuilder();
        for (String s : split) {
            Map<String, String> tableAndAction = sqlParseManager.parseTableAndAction(s);
            if (tableAndAction != null && !tableAndAction.isEmpty()) {
                String action = tableAndAction.getOrDefault(DbParseConstants.ACTION, "");
                String tableName = tableAndAction.getOrDefault(DbParseConstants.TABLE, "");
                operationName.append(tableName).append("-").append(action).append(";");
            }
        }
        String string = operationName.toString();
        assertNotEquals("", string);
    }

    @Test
    public void testDelete() throws JSQLParserException {
        String sql = "DELETE \n" +
                "FROM Exam\n" +
                "WHERE S_date NOT IN \n" +
                "   (\n" +
                "      SELECT \n" +
                "         e2.maxdt\n" +
                "      FROM \n" +
                "         (\n" +
                "            SELECT \n" +
                "               Order_Id, Product_Id, Amt, MAX(S_date) AS maxdt\n" +
                "            FROM TEST\n" +
                "            GROUP BY \n" +
                "               Order_Id, \n" +
                "               Product_Id, \n" +
                "               Amt\n" +
                "         )  AS e2\n" +
                "   );";

        Map<String, String> tableAndAction = sqlParseManager.parseTableAndAction(sql);
        assertEquals("Exam,TEST", tableAndAction.get(DbParseConstants.TABLE));
        assertEquals(DbParseConstants.DELETE, tableAndAction.get(DbParseConstants.ACTION));
    }


    @Test
    public void testUpdate() throws JSQLParserException {
        String sql = "UPDATE Websites \n" +
                "SET alexa='5000', country='USA' \n" +
                "WHERE name='菜鸟教程';";

        Map<String, String> tableAndAction = sqlParseManager.parseTableAndAction(sql);
        assertEquals("Websites", tableAndAction.get(DbParseConstants.TABLE));
        assertEquals(DbParseConstants.UPDATE, tableAndAction.get(DbParseConstants.ACTION));
    }

    @Test
    public void testInsert() throws JSQLParserException {
        String sql = "INSERT INTO category_stage (\n" +
                "   SELECT \n" +
                "      *\n" +
                "   FROM category );";
        Map<String, String> tableAndAction = sqlParseManager.parseTableAndAction(sql);
        assertEquals("category_stage,category", tableAndAction.get(DbParseConstants.TABLE));
        assertEquals(DbParseConstants.INSERT, tableAndAction.get(DbParseConstants.ACTION));
    }

    @Test
    public void testReplace() throws JSQLParserException {
        String sql = "replace into tb1(name, title, mood) select  rname, rtitle, rmood from tb2";
        Map<String, String> tableAndAction = sqlParseManager.parseTableAndAction(sql);
        assertEquals("tb1,tb2", tableAndAction.get(DbParseConstants.TABLE));
        assertEquals(DbParseConstants.REPLACE, tableAndAction.get(DbParseConstants.ACTION));
    }

    @Test
    public void test() throws JSQLParserException {
        String sql = "SELECT \n         t.id, \n         t.task_id, \n         t.uid, \n         p.id project_id, \n         " +
                "p.channel_code, \n         t.task_status, \n         t.current_target, \n         t.received_time, \n        " +
                " t.expired_time, \n         t.completed_time, \n         t.award_time, \n         t.times, \n         t.canceled_time, \n         " +
                "t.serial_number,\n         t.second_id,\n         t.second_type\n   FROM tk_user_received_task_21 t \n      " +
                "INNER JOIN tk_project  p ON t.project_id = p.id\n   WHERE t.uid = ? AND t.id In (?) ";
        // 去除所有空白字符
        sql = sql.replaceAll("\\s+", " ");
        Map<String, String> tableAndAction = sqlParseManager.parseTableAndAction(sql);
        assertEquals("tk_user_received_task_21,tk_project", tableAndAction.get(DbParseConstants.TABLE));
        assertEquals(DbParseConstants.SELECT, tableAndAction.get(DbParseConstants.ACTION));
    }
}
