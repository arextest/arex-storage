package com.arextest.storage.model.replay;

import com.arextest.storage.model.Response;
import com.arextest.storage.model.header.ResponseStatusType;
import com.arextest.storage.model.replay.holder.ListResultHolder;
import lombok.Data;

import java.util.List;

/**
 * @author jmo
 * @since 2021/11/2
 */
@Data
public class QueryReplayResultResponseType implements Response {
    private ResponseStatusType responseStatusType;
    private List<ListResultHolder<String>> resultHolderList;
}
