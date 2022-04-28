package com.arextest.storage.model.replay;

import com.arextest.storage.model.enums.RecordEnvType;
import lombok.Data;

/**
 * @author jmo
 * @since 2021/11/2
 */
@Data
public class ReplayCaseRangeRequestType {
    private String appId;
    private Long beginTime;
    private Long endTime;
    /**
     * 生产：0
     * 测试：1
     *
     * @see RecordEnvType
     */
    private Integer env;
    private int maxCaseCount;
    private String service;
    private String operation;
    private String subject;
    private String agentRecordVersion;
    /**
     * The main entry category type,others not allowed.
     */
    private Integer categoryType;
}
