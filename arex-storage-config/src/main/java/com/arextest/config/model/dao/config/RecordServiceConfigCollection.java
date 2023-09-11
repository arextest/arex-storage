package com.arextest.config.model.dao.config;

import java.util.Map;
import java.util.Set;

import com.arextest.config.model.dao.BaseEntity;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldNameConstants;

@Data
@NoArgsConstructor
@FieldNameConstants
public class RecordServiceConfigCollection extends BaseEntity {

    public static final String DOCUMENT_NAME = "RecordServiceConfig";

    @NonNull
    private String appId;

    private int sampleRate;

    private int allowDayOfWeeks;

    private boolean timeMock;
    @NonNull
    private String allowTimeOfDayFrom;
    @NonNull
    private String allowTimeOfDayTo;

    private Set<String> excludeServiceOperationSet;

    private Integer recordMachineCountLimit;

    private Map<String, String> extendField;
}
