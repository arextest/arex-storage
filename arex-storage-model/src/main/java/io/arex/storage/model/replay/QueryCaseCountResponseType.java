package io.arex.storage.model.replay;

import io.arex.storage.model.Response;
import io.arex.storage.model.header.ResponseStatusType;
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
