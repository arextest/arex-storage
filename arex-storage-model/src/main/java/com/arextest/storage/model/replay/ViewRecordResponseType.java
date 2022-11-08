package com.arextest.storage.model.replay;

import com.arextest.storage.model.Response;
import com.arextest.storage.model.header.ResponseStatusType;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @author jmo
 * @since 2021/11/3
 */
@Data
public class ViewRecordResponseType implements Response {
    private ResponseStatusType responseStatusType;
    private Map<Integer, List<String>> recordResult;
}