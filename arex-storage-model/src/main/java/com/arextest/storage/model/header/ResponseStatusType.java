package com.arextest.storage.model.header;

import com.arextest.storage.model.ResponseCode;
import lombok.Data;

/**
 * @author jmo
 * @since 2021/11/2
 */
@Data
public class ResponseStatusType {
    /**
     * The response code is 0 means everything ok, others means wrong.
     */
    private Integer responseCode;
    /**
     * The kindly of message when response code is not 0.
     */
    private String responseDesc;
    /**
     * The utc time stamp
     */
    private Long timestamp;

    public boolean hasError() {
        return this.responseCode == null || this.responseCode != ResponseCode.SUCCESS.getCodeValue();
    }
}
