package com.arextest.storage.mock;

import lombok.Getter;
import lombok.Setter;

public class MockResultContext {

  @Setter
  @Getter
  private boolean lastOfResult = false;

  @Setter
  @Getter
  private byte[] valueRefKey;

  @Getter
  private final MockResultMatchStrategy mockStrategy;

  public MockResultContext(MockResultMatchStrategy mockStrategy) {
    this.mockStrategy = mockStrategy;
  }
}