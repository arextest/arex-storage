package com.arextest.model.replay;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateCaseStatusRequestType {
    private String recordId;
    /**
     * @see CaseStatusEnum
     */
    private Integer caseStatus;
}