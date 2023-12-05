package com.arextest.storage.mock;

import com.arextest.diff.model.eigen.EigenOptions;
import com.arextest.diff.model.eigen.EigenResult;
import com.arextest.diff.sdk.EigenSDK;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Calculates the eigen value of the body.
 * created by xinyuan_wang on 2023/11/10
 */
public class EigenProcessor {

  private static final EigenSDK eigenSDK = new EigenSDK();

  public static Map<Integer, Long> calculateEigen(String body, String categoryName,
      Collection<List<String>> exclusions,
      Collection<String> ignoreNodes) {
    EigenOptions options = EigenOptions.options();
    options.putCategoryType(categoryName);
    if (CollectionUtils.isNotEmpty(exclusions)) {
      options.putExclusions(exclusions);
    }
    if (CollectionUtils.isNotEmpty(ignoreNodes)) {
      options.putIgnoreNodes(ignoreNodes);
    }
    EigenResult eigenResult = eigenSDK.calculateEigen(body, options);
    if (eigenResult == null) {
      return null;
    }
    return eigenResult.getEigenMap();
  }
}