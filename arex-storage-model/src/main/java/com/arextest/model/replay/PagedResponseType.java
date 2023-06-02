package com.arextest.model.replay;

import com.arextest.model.response.ResponseStatusType;
import com.arextest.model.mock.AREXMocker;
import com.arextest.model.response.Response;
import lombok.Data;

import java.util.List;

/**
 * @author jmo
 * @since 2021/11/2
 */
@Data
public class PagedResponseType implements Response {
    private ResponseStatusType responseStatusType;
    private List<AREXMocker> records;
    private Long totalCount;
}