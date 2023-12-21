package com.arextest.model.mock;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author wildeslam.
 * @create 2023/12/20 15:49
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SplitAREXMocker extends AREXMocker {
    private Integer index;
}
