package com.arextest.storage.mock;

import lombok.Getter;

@Getter
public enum MockResultMatchStrategy {
  /**
   * if the number of visits is greater than the number stored in the records, try to find the last
   * value as mock response
   */
  TRY_FIND_LAST_VALUE(0),
  /**
   * if the number of visits is greater than the number stored in the records,don't use last value
   * as mock response,should be return null
   */
  BREAK_RECORDED_COUNT(1),
  /**
   * Strict matching, Null if not matched
   */
  STRICT_MATCH(2);
  private final int strategyCode;

  MockResultMatchStrategy(int strategyCode) {
    this.strategyCode = strategyCode;
  }

  /**
   * default return TRY_FIND_LAST_VALUE
   */
  public static MockResultMatchStrategy of(int strategyCode) {
    MockResultMatchStrategy[] values = MockResultMatchStrategy.values();
    for (MockResultMatchStrategy returnStrategy : values) {
      if (returnStrategy.getStrategyCode() == strategyCode) {
        return returnStrategy;
      }
    }
    return TRY_FIND_LAST_VALUE;
  }
}