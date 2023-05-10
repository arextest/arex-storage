package com.arextest.model.replay;

import com.arextest.model.response.Response;
import com.arextest.model.response.ResponseStatusType;
import lombok.Data;

import java.util.List;

@Data
public class CountRecordCaseResponseType implements Response {
    private ResponseStatusType responseStatusType;

    private long recordedCaseCount;

    private List<String> operationNameList;
}
