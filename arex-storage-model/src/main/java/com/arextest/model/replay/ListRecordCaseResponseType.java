package com.arextest.model.replay;

import com.arextest.model.mock.AREXMocker;
import com.arextest.model.response.Response;
import com.arextest.model.response.ResponseStatusType;
import lombok.Data;

import java.util.List;

@Data
public class ListRecordCaseResponseType implements Response {
    private ResponseStatusType responseStatusType;

    /**
     * for paging
     * only First-Page will return
     */
    private long totalCount;

    private List<AREXMocker> recordList;
}
