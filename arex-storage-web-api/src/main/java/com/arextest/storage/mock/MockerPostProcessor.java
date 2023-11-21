package com.arextest.storage.mock;

import com.arextest.common.utils.JsonTraverseUtils;
import com.arextest.model.mock.Mocker;
import com.arextest.storage.utils.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import java.util.Optional;

public class MockerPostProcessor {

  public static void desensitize(List<Mocker> allReadableResult)
      throws JsonProcessingException {
    for (Mocker arexMocker : allReadableResult) {
      Mocker.Target request = arexMocker.getTargetRequest();
      Mocker.Target response = arexMocker.getTargetResponse();

      if (JsonUtil.isJsonStr(request.getBody())) {
        request.setBody(JsonTraverseUtils.trimAllLeaves(request.getBody()));
      }
      if (JsonUtil.isJsonStr(response.getBody())) {
        response.setBody(JsonTraverseUtils.trimAllLeaves(response.getBody()));
      }
      Optional.ofNullable(response.getAttributes())
          .ifPresent(attributes -> attributes.entrySet().forEach(entry -> entry.setValue(null)));
      Optional.ofNullable(request.getAttributes())
          .ifPresent(attributes -> attributes.entrySet().forEach(entry -> entry.setValue(null)));
    }
  }
}
