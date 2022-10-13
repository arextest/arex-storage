package com.arextest.storage.model.replay;

import lombok.Data;

/**
 * Created by rchen9 on 2022/10/11.
 */
@Data
public class QueryRecordRequestType {
    private String recordId;
    /**
     * The bits shift from categoryType value, means the response should be include.
     * zero means all
     * example:
     * soaMain codeValue=0,then 1<<0
     * soaExternal codeValue=1,then 1<<1
     */
    private Long categoryTypes;
}
