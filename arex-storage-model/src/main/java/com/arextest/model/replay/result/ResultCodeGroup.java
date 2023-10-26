package com.arextest.model.replay.result;

import java.util.List;
import lombok.Data;

@Data
public class ResultCodeGroup {

  private List<CategoryGroup> categoryGroups;
  private int resultCode;

  @Data
  public static class CategoryGroup {

    private String categoryName;
    private List<IdPair> resultIds;
  }

  @Data
  public static class IdPair {

    private String recordId;
    private String targetId;
  }
}
