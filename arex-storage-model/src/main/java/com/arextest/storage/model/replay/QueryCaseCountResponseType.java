package com.arextest.storage.model.replay;

import com.arextest.storage.model.Response;
import com.arextest.storage.model.header.ResponseStatusType;
import lombok.Data;

/**
 * @author jmo
 * @since 2021/11/3
 */
@Data
public class QueryCaseCountResponseType implements Response {
    private ResponseStatusType responseStatusType;
    private Integer count;
}
