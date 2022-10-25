package com.arextest.storage.core.mock;

import com.arextest.storage.model.mocker.impl.DatabaseMocker;
import com.fasterxml.jackson.databind.ObjectMapper;
import mockit.Injectable;
import mockit.Tested;
import org.junit.Assert;

import java.util.List;

public class DbMockKeyBuilderTest {
    @Tested
    private DbMockKeyBuilder dbMockKeyBuilder;
    @Injectable
    private ObjectMapper objectMapper;

    public void dbMockKeyBuildTest() {
        DatabaseMocker dalResultMocker = new DatabaseMocker();
        String sqlText = "SELECT a.runoob_id, a.runoob_author, b.runoob_count FROMX db.`A` a, `B` b " +
                "WHERE" +
                " a.runoob_author = b.runoob_author\n" +
                "SELECT a.runoob_id, a.runoob_author, b.runoob_count FROM runoob_tbl1 a INNER JOIN `[tcount_tbl]2` ,w" +
                " " +
                "ON a.runoob_author = b.runoob_author;\n" +
                "SELECT a.runoob_id, a.runoob_author, b.runoob_count FROM runoob_tbl3 INNER JOIN tcount_tbl4 bd  ,w " +
                "ON " +
                "a.runoob_author = b.runoob_author;\n" +
                "SELECT a.runoob_id, a.runoob_author, b.runoob_count FROM runoob_tbl5 ;\n" +
                "DELETE FROM runoob_tbl6 where  runoob_id=3;\n" +
                "UPDATE update_runoob_tbl7 SET runoob_title='C++' WHERE runoob_id=3;\n" +
                "INSERT INTO INSERT_runoob_tbl8 (runoob_title, runoob_author, submission_date)\n" +
                "SELECT a.runoob_id, a.runoob_author, b.runoob_count FROM  dbo.runoob_tbl9;\n" +
                "SELECT a.runoob_id, a.runoob_author, b.runoob_count FROM  `runoob_tbl10`;\n" +
                "SELECT a.runoob_id, a.runoob_author, b.runoob_count FROM  dbo.[runoob_tbl11] a;\n" +
                "SELECT a.runoob_id, a.runoob_author, b.runoob_count FROM  dbo.[runoob_tbl12] as a;\n";
        dalResultMocker.setDbName("TestDb");
        dalResultMocker.setSql(sqlText);
        dalResultMocker.setParameters("[{\"ExternalNo\":\"31f4298f20dd4b4cbae1ad141a4136eb\"}]");

        List<byte[]> result = dbMockKeyBuilder.dbMockKeyBuild(dalResultMocker);
        Assert.assertNotNull(result);
        Assert.assertEquals(2, result.size());
        byte[] tableHashCode = new byte[]{64, 11, -33, 43, -102, 83, 28, 7, 55, -65, -46, 108, 103, 109, 20, 35};
        for (int i = 0; i < tableHashCode.length; i++) {
            byte b = tableHashCode[i];
            Assert.assertEquals(b, result.get(1)[i]);
        }
    }
}