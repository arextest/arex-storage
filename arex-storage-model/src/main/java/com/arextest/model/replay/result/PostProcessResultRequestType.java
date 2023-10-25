package com.arextest.model.replay.result;

import java.util.List;
import lombok.Data;

/**
 * @author qzmo
 * @since 2023/09/19
 */
@Data
public class PostProcessResultRequestType {

  private String replayPlanId;
  private int replayStatusCode;
  private List<ResultCodeGroup> results;
}