package com.arextest.storage.model.replay;

import com.arextest.storage.model.Response;
import com.arextest.storage.model.header.ResponseStatusType;
import lombok.Data;

/**
 * @author jmo
 * @since 2021/11/8
 */
@Data
public class QueryMockCacheResponseType implements Response {
    private ResponseStatusType responseStatusType;
}