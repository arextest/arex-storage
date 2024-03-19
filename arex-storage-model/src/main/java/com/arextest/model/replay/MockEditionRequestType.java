package com.arextest.model.replay;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author jmo
 * @since 2021/11/3
 */
@Getter
@Setter
@ToString
public class MockEditionRequestType {

  private String appId;
  private String recordId;
  private String targetRecordId;
  private String srcProviderName;
  private String targetProviderName;
  private String category;
}