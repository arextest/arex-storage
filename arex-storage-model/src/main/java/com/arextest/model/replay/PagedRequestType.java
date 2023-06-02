package com.arextest.model.replay;

import com.arextest.model.mock.MockCategoryType;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class PagedRequestType {
    private String appId;
    private Long beginTime;
    private Long endTime;
    private Integer env;
    private int pageSize;
    private Integer pageIndex;
    private String operation;
    private MockCategoryType category;
    private String sourceProvider;

    /**
     * need to filter non latest versions or not.
     * old version default value: true
     */
    private Boolean filterPastRecordVersion;

    /**
     * Order by CreationTime.
     * null:no usage 1:desc 2:asc
     * default : 2
     */
    private Integer creationTimeOrder;

    private Boolean needTotal;
}