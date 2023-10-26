package com.arextest.model.replay;

import com.arextest.model.response.Response;
import com.arextest.model.response.ResponseStatusType;
import java.util.Map;
import lombok.Data;

@Data
public class CountOperationCaseResponseType implements Response {

  private ResponseStatusType responseStatusType;
  private Map<String, Long> countMap;
}
