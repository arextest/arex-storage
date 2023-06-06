package com.arextest.model.replay;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class SortingOption {
    /**
     * order by.
     */
    private String label;

    /**
     * @see SortingTypeEnum
     * desc or asc
     */
    private Integer sortingType;
}
