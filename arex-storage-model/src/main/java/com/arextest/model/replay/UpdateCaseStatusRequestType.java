package com.arextest.model.replay;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateCaseStatusRequestType {
    @NotBlank(message = "recordId cannot be blank")
    private String recordId;
    /**
     * @see CaseStatusEnum
     */
    @NotNull(message = "caseStatus cannot be null")
    private Integer caseStatus;
}