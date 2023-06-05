package com.arextest.model.replay;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class OrderCondition {
    /**
     * order by.
     */
    private String orderKey;

    /**
     * @see OrderMethod
     * desc or asc
     */
    private Integer orderMethod;
}
