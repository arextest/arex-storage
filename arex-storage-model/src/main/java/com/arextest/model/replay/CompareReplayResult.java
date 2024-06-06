package com.arextest.model.replay;

import com.arextest.model.mock.MockCategoryType;
import lombok.Data;

/**
 * created by xinyuan_wang on 2023/06/04
 */
@Data
public class CompareReplayResult {

  private MockCategoryType categoryType;
  private String operationName;
  private String recordId;
  private String replayId;
  /**
   * record request message
   */
  private String baseMsg;
  /**
   * replay request message
   */
  private String testMsg;
  private long recordTime;
  private long replayTime;
  private String appId;
  /**
   * Check whether the recording and playback request messages are consistent.
   * If they are consistent, only baseMsg needs to be passed.
   */
  private boolean sameMsg;
}