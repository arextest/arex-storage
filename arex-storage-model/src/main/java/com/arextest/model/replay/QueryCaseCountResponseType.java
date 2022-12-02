package com.arextest.model.replay;

import com.arextest.model.response.Response;
import com.arextest.model.response.ResponseStatusType;
import lombok.Data;

/**
 * @author jmo
 * @since 2021/11/3
 */
@Data
public class QueryCaseCountResponseType implements Response {
    private ResponseStatusType responseStatusType;
    private long count;
}