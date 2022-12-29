package com.arextest.model.replay;

import lombok.Data;

/**
 * @author jmo
 * @since 2021/11/8
 */
@Data
public class QueryMockCacheRequestType {
    private String recordId;
    private String sourceProvider;
}