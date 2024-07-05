package com.arextest.storage.model;


import java.util.List;
import java.util.Set;

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
    Set<String> tableNames;
    /**
     * eg: query/insert/update/delete
     */
    String action;
}
