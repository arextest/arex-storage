package io.arex.storage.model.replay;

import io.arex.storage.model.Response;
import io.arex.storage.model.header.ResponseStatusType;
import io.arex.storage.model.replay.holder.ListResultHolder;
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
