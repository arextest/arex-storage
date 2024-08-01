package com.arextest.model.replay;


import com.arextest.model.response.Response;
import com.arextest.model.response.ResponseStatusType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateCaseStatusResponseType implements Response {
    private ResponseStatusType responseStatusType;
    private long body;
}