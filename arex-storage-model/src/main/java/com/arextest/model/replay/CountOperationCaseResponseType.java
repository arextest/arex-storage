package com.arextest.model.replay;

import com.arextest.model.response.Response;
import com.arextest.model.response.ResponseStatusType;
import lombok.Data;

import java.util.Map;

@Data
public class CountOperationCaseResponseType implements Response {
    private ResponseStatusType responseStatusType;
    private Map<String, Long> countMap;
}
