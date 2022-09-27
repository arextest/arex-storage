package com.arextest.storage.core.mock;

import com.arextest.storage.core.cache.CacheKeyUtils;
import com.arextest.storage.model.mocker.impl.DalResultMocker;
import com.arextest.storage.model.mocker.impl.DatabaseMocker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Why the db mock key has more items building?
 * 1,The user add some comments to sql or
 * 2,add or remove columns from select or
 * 3,The value of parameter is random or
 * 4,The sql parameter's order changed or
 * 5,others
 * we should try fork/mock a result as return for replaying don't break user's workflow.
 * after that,the comparison will show all differences.
 * NOTE:The mock result not necessarily correct for current requested.
 *
 * @author jmo
 * @since 2021/11/25
 */
@Slf4j
@Component
final class DbMockKeyBuilder {
    @Resource
    private ObjectMapper objectMapper;
    private static final byte[] EMPTY_BYTE = new byte[]{};
    private static final char SQL_BATCH_TERMINAL_CHAR = ';';
    private static final int INDEX_NOT_FOUND = -1;
    private static final int UPPER_LOWER_CASE_DELTA_VALUE = 32;
    private static final String TIDY_PREFIX = ",";
    /**
     * table name for ms-sql-server and mysql, which valid format as follow:
     * ms-sql-server example:
     * 1,dbo.[tableName]
     * 2,[orderDb].dbo.[tableName]
     * 3,tableName
     * mysql example:
     * 1,`orderDb`.`tableName'
     * 2,`tableName'
     * 3, tableName
     * <p>
     * table name for inner join as short,as follow:
     * SELECT * FROM db.`tableNameA` a, tableNameB` b WHERE a.id = b.id
     * <p>
     * for example:
     * SELECT * FROM tableNameA a INNER JOIN `tableNameB` b ON a.id = b.id;
     * SELECT * FROM tableNameA a LEFT JOIN db.tableNameB b ON a.id = b.id;
     * SELECT * FROM tableNameA a RIGHT JOIN tableNameB b ON a.id = b.id;
     */
    private static final List<String> SQL_TABLE_KEYS = Lists.newArrayList("from", "join", "update", "into");

    List<byte[]> dataBaseMockKeyBuild(DatabaseMocker instance) {
        List<byte[]> mockKeyList = new ArrayList<>(MockKeyBuilder.MAX_MOCK_KEY_CAPACITY);
        // First,add full match as mock key
        String sqlParameter = instance.getParameters();
        String sqlText = instance.getSql();
        byte[] dbNameBytes = CacheKeyUtils.toUtf8Bytes(instance.getDbName());
        byte[] sqlTextBytes = CacheKeyUtils.toUtf8Bytes(sqlText);
        byte[] methodNameBytes = StringUtils.isEmpty(instance.getMethodName()) ? EMPTY_BYTE :
                CacheKeyUtils.toUtf8Bytes(instance.getMethodName());
        byte[] sqlParameterBytes = StringUtils.isEmpty(sqlParameter) ? EMPTY_BYTE :
                CacheKeyUtils.toUtf8Bytes(sqlParameter);
        MessageDigest md5Digest = MockKeyBuilder.getMD5Digest();
        md5Digest.update(dbNameBytes);
        md5Digest.update(sqlTextBytes);
        md5Digest.update(methodNameBytes);
        md5Digest.update(sqlParameterBytes);
        byte[] hashValue = md5Digest.digest();
        md5Digest.reset();
        mockKeyList.add(hashValue);
        // secondly,add all db table names & all parameters' names as mock key
        md5Digest.update(dbNameBytes);
        md5Digest.update(methodNameBytes);
        findTableNameToMd5(sqlText, md5Digest);
        if (sqlParameterBytes != EMPTY_BYTE) {
            tryAddParameterWithoutValue(md5Digest, sqlParameter);
        }
        hashValue = md5Digest.digest();
        md5Digest.reset();
        mockKeyList.add(hashValue);

        return mockKeyList;
    }

    private boolean tryAddParameterWithoutValue(MessageDigest md5Digest, String sqlParameter) {
        try {
            JsonNode jsonNode = objectMapper.readTree(sqlParameter);
            if (jsonNode.isEmpty()) {
                return false;
            }
            Iterator<JsonNode> iterator = jsonNode.elements();
            while (iterator.hasNext()) {
                Iterator<String> fieldNames = iterator.next().fieldNames();
                while (fieldNames.hasNext()) {
                    String fieldName = fieldNames.next();
                    fieldName = StringUtils.lowerCase(fieldName);
                    md5Digest.update(CacheKeyUtils.toUtf8Bytes(fieldName));
                }
            }
            return true;
        } catch (JsonProcessingException e) {
            LOGGER.warn("tryAddParameterWithoutValue error:{},sqlParameter:{}", e.getMessage(), sqlParameter, e);
            return false;
        }
    }

    private void findTableNameToMd5(String sqlText, MessageDigest md5Digest) {
        int sourceCount = sqlText.length();
        for (int i = 0; i < SQL_TABLE_KEYS.size(); i++) {
            String key = SQL_TABLE_KEYS.get(i);
            int targetCount = key.length();
            int fromIndex = 0;
            int index = findIndexWholeIgnoreCase(sqlText, sourceCount, key, targetCount, fromIndex);
            while (index != INDEX_NOT_FOUND) {
                fromIndex = index + targetCount;
                int skipWhitespaceCount = skipWhitespace(sqlText, fromIndex, sourceCount);
                fromIndex += skipWhitespaceCount;
                String value = readTableValue(sqlText, fromIndex, sourceCount);
                int valueLength = value.length();
                fromIndex += valueLength;
                md5Digest.update(CacheKeyUtils.toUtf8Bytes(value));
                index = findIndexWholeIgnoreCase(sqlText, sourceCount, key, targetCount, fromIndex);
            }
        }
    }

    private String readTableValue(String sqlText, int readFromIndex, int sourceCount) {
        final int valueBeginIndex = readFromIndex;
        for (; readFromIndex < sourceCount; readFromIndex++) {
            if (readShouldTerminal(sqlText.charAt(readFromIndex))) {
                break;
            }
        }
        return sqlText.substring(valueBeginIndex, readFromIndex);
    }

    private static boolean readShouldTerminal(char src) {
        return src == SQL_BATCH_TERMINAL_CHAR || isWhitespace(src);
    }

    private static int skipWhitespace(String sqlText, int fromIndex, int sourceCount) {
        int skipWhitespaceCount = 0;
        for (; fromIndex < sourceCount && isWhitespace(sqlText.charAt(fromIndex)); fromIndex++, skipWhitespaceCount++) {

        }
        return skipWhitespaceCount;
    }

    private static boolean isWhitespace(char src) {
        return Character.isWhitespace(src);
    }

    private int findIndexWholeIgnoreCase(String source, int sourceCount, String target, int targetCount,
                                         int fromIndex) {
        if (fromIndex >= sourceCount) {
            return INDEX_NOT_FOUND;
        }
        char first = target.charAt(0);
        int max = sourceCount - targetCount;
        for (int i = fromIndex; i <= max; i++) {
            if (firstCharacterWordBoundaryNotMatch(source, first, i)) {
                while (++i <= max && firstCharacterWordBoundaryNotMatch(source, first, i)) {

                }
            }
            //  Found first character, now look at the rest of target
            if (i <= max) {
                int j = i + 1;
                int end = j + targetCount - 1;
                for (int k = 1; j < end && equalsIgnoreCase(source.charAt(j), target.charAt(k)); j++, k++) {

                }

                if (j == end && isWordBoundary(source, j)) {
                    /* Found whole string. */
                    return i;
                }
            }
        }
        return INDEX_NOT_FOUND;
    }

    private static boolean firstCharacterWordBoundaryNotMatch(final String source, char first, int position) {
        return !(equalsIgnoreCase(source.charAt(position), first) && isWordBoundary(source, position - 1));
    }

    private static boolean isWordBoundary(final String source, int positionOfPrevOrNext) {
        if (positionOfPrevOrNext < 0 || positionOfPrevOrNext >= source.length()) {
            return true;
        }
        return isWhitespace(source.charAt(positionOfPrevOrNext));
    }

    private static boolean equalsIgnoreCase(char src, char target) {
        return src == target || Math.abs(src - target) == UPPER_LOWER_CASE_DELTA_VALUE;
    }
}
