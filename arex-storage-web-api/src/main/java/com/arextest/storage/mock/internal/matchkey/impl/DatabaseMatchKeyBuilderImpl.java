package com.arextest.storage.mock.internal.matchkey.impl;

import com.arextest.model.constants.MockAttributeNames;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker;
import com.arextest.storage.cache.CacheKeyUtils;
import com.arextest.storage.mock.MatchKeyBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

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
@Order(30)
final class DatabaseMatchKeyBuilderImpl implements MatchKeyBuilder {
    private final ObjectMapper objectMapper;

    private static final char SQL_BATCH_TERMINAL_CHAR = ';';
    private static final int INDEX_NOT_FOUND = -1;
    private static final int UPPER_LOWER_CASE_DELTA_VALUE = 32;
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
    private static final List<String> SQL_TABLE_KEYS = Arrays.asList("from", "join", "update", "into");

    DatabaseMatchKeyBuilderImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean isSupported(MockCategoryType categoryType) {
        return Objects.equals(categoryType, MockCategoryType.DATABASE);
    }

    @Override
    public List<byte[]> build(Mocker databaseMocker) {
        return dbMockKeyBuild(databaseMocker);
    }

    /**
     * 1,db+sql+parameterNameWithValue+operationName
     * 2,db+table+parameterName+operationName
     * 3,db+table+operationName
     *
     * @param databaseMocker the db mocker
     * @return all mock keys
     */
    private List<byte[]> dbMockKeyBuild(Mocker databaseMocker) {
        List<byte[]> keys = new ArrayList<>();
        Mocker.Target targetRequest = databaseMocker.getTargetRequest();
        String sqlParameter = targetRequest.attributeAsString(MockAttributeNames.DB_PARAMETERS);
        String sqlText = targetRequest.getBody();
        String dbName = targetRequest.attributeAsString(MockAttributeNames.DB_NAME);
        byte[] dbNameBytes = CacheKeyUtils.toUtf8Bytes(dbName);
        byte[] sqlTextBytes = CacheKeyUtils.toUtf8Bytes(sqlText);
        byte[] sqlParameterBytes = CacheKeyUtils.toUtf8Bytes(sqlParameter);
        byte[] operationBytes = CacheKeyUtils.toUtf8Bytes(databaseMocker.getOperationName());
        MessageDigest md5Digest = MessageDigestWriter.getMD5Digest();
        md5Digest.update(dbNameBytes);
        md5Digest.update(operationBytes);
        byte[] dbNameMatchKey = md5Digest.digest();

        md5Digest.update(sqlTextBytes);
        md5Digest.update(sqlParameterBytes);
        md5Digest.update(dbNameBytes);
        md5Digest.update(operationBytes);
        // 1,db+sql+parameterNameWithValue+operationName
        byte[] fullMatchKey = md5Digest.digest();
        keys.add(fullMatchKey);

        findTableNameToMd5(sqlText, md5Digest);
        md5Digest.update(dbNameMatchKey);
        // 3,db+table+operationName
        byte[] tableMatchKey = md5Digest.digest();

        if (StringUtils.isNotEmpty(sqlParameter) && tryAddParameterWithoutValue(md5Digest, sqlParameter)) {
            md5Digest.update(tableMatchKey);
            byte[] tableWithParametersMatchKey = md5Digest.digest();
            // 2,db+table+parameterName+operationName
            keys.add(tableWithParametersMatchKey);
        }
        keys.add(tableMatchKey);
        return keys;
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

        } catch (JsonProcessingException e) {
            LOGGER.warn("tryParseParameterAsJson error:{},sqlParameter:{}", e.getMessage(), sqlParameter, e);
        }
        return true;

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