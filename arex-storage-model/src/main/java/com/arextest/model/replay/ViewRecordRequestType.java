package com.arextest.model.replay;

import lombok.Getter;
import lombok.Setter;

/**
 * @author jmo
 * @since 2021/11/3
 */
@Getter
@Setter
public class ViewRecordRequestType {
    private String recordId;
    private String sourceProvider;
    private String categoryType;
}