package com.arextest.model.replay;

import lombok.Data;

/**
 * @author: xinyuan_wang
 * @date: 2023/05/07
 **/
@Data
public class QueryMockRequestType {
    private String recordId;
    private String[] categoryTypes;
    private String[] fieldNames;
}
