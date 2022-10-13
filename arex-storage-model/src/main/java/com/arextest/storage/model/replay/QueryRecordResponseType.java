package com.arextest.storage.model.replay;

import com.arextest.storage.model.Response;
import com.arextest.storage.model.header.ResponseStatusType;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Created by rchen9 on 2022/10/11.
 */
@Data
public class QueryRecordResponseType implements Response {
    private ResponseStatusType responseStatusType;
    private Map<Integer, List<String>> recordResult;
}
