package io.arex.storage.model.replay;

import lombok.Data;

/**
 * @author jmo
 * @since 2021/11/2
 */
@Data
public class QueryReplayResultRequestType {
    private String recordId;
    private String replayResultId;
}
