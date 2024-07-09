package com.arextest.storage.model;


import java.util.List;
import lombok.Data;

/**
 * @since 2024/5/10
 */
@Data
public class TableSchema {
    String dbName;
    /**
     * Joint query sql statement parses out multiple table names
     */
    List<String> tableNames;
    /**
     * eg: query/insert/update/delete
     */
    String action;
}
