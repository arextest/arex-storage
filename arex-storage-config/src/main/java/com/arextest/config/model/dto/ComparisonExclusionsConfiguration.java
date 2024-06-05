package com.arextest.config.model.dto;

import java.util.Date;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @author wildeslam.
 * @create 2024/5/29 16:40
 */
@Data
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class ComparisonExclusionsConfiguration extends AbstractConfiguration {

  private String id;

  /**
   * optional when compareConfigType = 1, appId is empty
   */
  private String appId;

  /**
   * optional The value limit to special operation should be used, else,couldn't apply for it. if it
   * is empty, it means is the global configuration of app That the configuration of app have the
   * meaning of global is "Exclusion"
   */
  private String operationId;

  /**
   * 0: pinned forever use it, 1: after timeout, still displayed but not work.
   */
  private int expirationType;
  private Date expirationDate;

  /**
   * 0: replay, 1: collection.
   */
  private int compareConfigType;

  /**
   * This value is valid only when {compareConfigType} = 1
   */
  private String fsInterfaceId;

  /**
   * for bo
   */
  private String dependencyId;

  /**
   * for vo
   */
  private String operationType;
  private String operationName;

  private List<String> exclusions;
}
