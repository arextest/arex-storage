package com.arextest.config.model.vo;

import java.util.List;
import java.util.Set;
import lombok.Data;

/**
 * @author wildeslam.
 * @create 2024/7/4 13:46
 */
@Data
public class CompareConfiguration {
  private List<ConfigComparisonExclusions> comparisonExclusions;
  private Set<List<String>> globalExclusionList;
  private Set<String> ignoreNodeSet;
}
