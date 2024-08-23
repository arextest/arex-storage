package com.arextest.storage.mock;

import com.arextest.common.utils.JsonTraverseUtils;
import com.arextest.model.mock.AREXMocker;
import com.arextest.model.mock.Mocker;
import com.arextest.storage.utils.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public class MockerPostProcessor {

  private static final String PATTERN_STRING = "^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)?$";
  private static final Pattern BASE_64_PATTERN = Pattern.compile(PATTERN_STRING);

  public static void desensitize(List<AREXMocker> allReadableResult)
      throws JsonProcessingException {
    for (AREXMocker arexMocker : allReadableResult) {
      Mocker.Target request = arexMocker.getTargetRequest();
      Mocker.Target response = arexMocker.getTargetResponse();

      request.setBody(handleBody(request.getBody()));
      response.setBody(handleBody(response.getBody()));
      Optional.ofNullable(response.getAttributes())
          .ifPresent(attributes -> attributes.entrySet().forEach(entry -> entry.setValue(null)));
      Optional.ofNullable(request.getAttributes())
          .ifPresent(attributes -> attributes.entrySet().forEach(entry -> entry.setValue(null)));
    }
  }

  private static String handleBody(String body) throws JsonProcessingException {
    if (JsonUtil.isJsonStr(body)) {
      return JsonTraverseUtils.trimAllLeaves(body);
    } else if (isBase64(body) && JsonUtil.isJsonStr(decodeBase64(body))) {
      return JsonTraverseUtils.trimAllLeaves(decodeBase64(body));
    } else {
      return null;
    }
  }

  private static boolean isBase64(String str) {
    return BASE_64_PATTERN.matcher(str).matches();
  }

  private static String decodeBase64(String str) {
    return new String(Base64.getDecoder().decode(str));
  }
}
