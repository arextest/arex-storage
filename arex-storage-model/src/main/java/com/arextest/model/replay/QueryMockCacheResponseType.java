package com.arextest.model.replay;

import com.arextest.model.response.Response;
import com.arextest.model.response.ResponseStatusType;
import lombok.Data;

/**
 * @author jmo
 * @since 2021/11/8
 */
@Data
public class QueryMockCacheResponseType implements Response {

  private ResponseStatusType responseStatusType;
}