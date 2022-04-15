package io.arex.storage.model.replay;

import lombok.Data;

/**
 * @author jmo
 * @since 2021/11/3
 */
@Data
public class ViewRecordRequestType {
    private String recordId;
    /**
     * The bits shift from categoryType value, means the response should be include.
     * zero means all
     * example:
     * soaMain codeValue=0,then 1<<0
     * soaExternal codeValue=1,then 1<<1
     */
    private Long categoryTypes;
}
