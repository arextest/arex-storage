package com.arextest.storage.utils;

import com.arextest.diff.handler.parse.sqlparse.constants.DbParseConstants;
import com.arextest.storage.model.TableSchema;
import com.arextest.storage.service.DatabaseParseService;
import net.sf.jsqlparser.statement.upsert.Upsert;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
/**
 * @author niyan
 * @date 2024/4/23
 * @since 1.0.0
 */
@RunWith(MockitoJUnitRunner.class)
public class DatabaseParseServiceTest {

    @InjectMocks
    DatabaseParseService databaseParseService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testSimpleSelect() {
        String sql = "SELECT * FROM mytable";
        TableSchema tableSchema = databaseParseService.parse(sql, "test");
        assertNotNull(tableSchema);
        assertEquals(DbParseConstants.SELECT.toLowerCase(), tableSchema.getAction().toLowerCase());
        assertEquals("mytable", StringUtils.join(tableSchema.getTableNames(), ","));
    }

    @Test
    public void testSelectWithJoin() {
        String sql = "select b.Name as Department,a.Name as Employee,a.Salary\n" +
                "from (select *,dense_rank() over(partition by departmentid order by Salary desc) as rnk from Employee) a \n"
                +
                "left join department b \n" +
                "on a.departmentid = b.Id and a.aa = b.aa and a.cc = b.cc\n" +
                "where a.rnk <= 3 and a.per_id in (select per_id from colle_subject);\n" +
                "\n";
        TableSchema tableSchema = databaseParseService.parse(sql, "test");
        assertNotNull(tableSchema);
        assertEquals(DbParseConstants.SELECT.toLowerCase(), tableSchema.getAction().toLowerCase());
        assertEquals("Employee,colle_subject,department", StringUtils.join(tableSchema.getTableNames(), ","));
    }

    @Test
    public void testMultiSimpleSelect() {
        String sql = "SELECT * FROM students WHERE score = 18; select * from student where score = 20;";
        String[] sqls = StringUtils.split(sql, ";");
        List<String> operationNames = new ArrayList<>(sqls.length);
        for (String s : sqls) {
            TableSchema tableSchema = databaseParseService.parse(s, "test");
            if (tableSchema == null) {
                continue;
            }
            operationNames.add(databaseParseService.regenerateOperationName(tableSchema, "operationName"));
        }
        assertNotEquals(0, operationNames.size());
        assertEquals("@students@Select@operationName", operationNames.get(0));
        assertEquals("@student@Select@operationName", operationNames.get(1));
    }

    @Test
    public void testDeleteWithSubSelect() {
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

        TableSchema tableSchema = databaseParseService.parse(sql, "test");
        assertNotNull(tableSchema);
        assertEquals("Exam,TEST", StringUtils.join(tableSchema.getTableNames(), ","));
        assertEquals(DbParseConstants.DELETE.toLowerCase(), tableSchema.getAction().toLowerCase());
    }


    @Test
    public void testUpdate() {
        String sql = "UPDATE Websites \n" +
                "SET alexa='5000', country='USA' \n" +
                "WHERE name='Tutorial';";

        TableSchema tableSchema = databaseParseService.parse(sql, "test");
        assertNotNull(tableSchema);
        assertEquals("Websites", StringUtils.join(tableSchema.getTableNames(), ","));
        assertEquals(DbParseConstants.UPDATE.toLowerCase(), tableSchema.getAction().toLowerCase());
    }

    @Test
    public void testInsertWithSubSelect() {
        String sql = "INSERT INTO category_stage (\n" +
                "   SELECT \n" +
                "      *\n" +
                "   FROM category );";
        TableSchema tableSchema = databaseParseService.parse(sql, "test");
        assertNotNull(tableSchema);
        assertEquals("category,category_stage", StringUtils.join(tableSchema.getTableNames(), ","));
        assertEquals(DbParseConstants.INSERT.toLowerCase(), tableSchema.getAction().toLowerCase());
    }

    @Test
    public void testReplaceWithSelect() {
        String sql = "replace into tb1(name, title, mood) select  rname, rtitle, rmood from tb2";
        TableSchema tableSchema = databaseParseService.parse(sql, "test");
        assertNotNull(tableSchema);
        assertEquals("tb1,tb2", StringUtils.join(tableSchema.getTableNames(), ","));
        assertEquals(Upsert.class.getSimpleName().toLowerCase(), tableSchema.getAction().toLowerCase());
    }

    @Test
    public void testSelectWithInnerJoin() {
        String sql = "SELECT \n         t.id, \n         t.task_id, \n         t.uid, \n         p.id project_id, \n         " +
                "p.channel_code, \n         t.task_status, \n         t.current_target, \n         t.received_time, \n        " +
                " t.expired_time, \n         t.completed_time, \n         t.award_time, \n         t.times, \n         t.canceled_time, \n         " +
                "t.serial_number,\n         t.second_id,\n         t.second_type\n   FROM tk_user_received_task_21 t \n      " +
                "INNER JOIN tk_project  p ON t.project_id = p.id\n   WHERE t.uid = ? AND t.id In (?) ";
        // 去除所有空白字符
        TableSchema tableSchema = databaseParseService.parse(sql, "test");
        assertNotNull(tableSchema);
        assertEquals("tk_project,tk_user_received_task_21", StringUtils.join(tableSchema.getTableNames(), ","));
        assertEquals(DbParseConstants.SELECT.toLowerCase(), tableSchema.getAction().toLowerCase());
    }

    @Test
    public void parseTableNames() {
        // test operationName is Empty
        List<String> tableNames = databaseParseService.parseTableNames("");
        assertEquals(0, tableNames.size());

        // test operationName contains one @
        tableNames = databaseParseService.parseTableNames("db1@table1");
        assertEquals(0, tableNames.size());

        // test table name contain empty
        tableNames = databaseParseService.parseTableNames("@@select@query");
        assertEquals(0, tableNames.size());

        tableNames = databaseParseService.parseTableNames("db1@table1,table2@select@operation1;db2@table3,table4@select@operation2");
        assertEquals("table1,table2", tableNames.get(0));
        assertEquals("table3,table4", tableNames.get(1));
    }

    @Test
    public void parseSQLWithSubSelect() {
        String sql = "select count(0) from ( select qi.code from qc_issue qi                 "
            + "where qi.enable = 1                                       "
            + "and (qi.title like \"%'?'%\" or qi.code like \"%\"?\"%\") "
            + "and qi.product_code in             (                  ?             )      "
            + "order by qi.id desc ) tmp_count";
        TableSchema tableSchema = databaseParseService.parse(sql, "test");
        assertEquals("qc_issue", StringUtils.join(tableSchema.getTableNames(), ","));
    }

    @Test
    public void parseSQLWithKeyWord() {
        String sql = "SELECT address,extended,union_user_id FROM access_token WHERE ( user_id = ? ) LIMIT ?";
        TableSchema tableSchema = databaseParseService.parse(sql, "test");
        assertEquals("access_token", StringUtils.join(tableSchema.getTableNames(), ","));
    }
}
