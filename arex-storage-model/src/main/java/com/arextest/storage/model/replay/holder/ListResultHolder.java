package com.arextest.storage.model.replay.holder;

import com.arextest.storage.model.enums.MockCategoryType;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

import java.util.List;

/**
 * @author jmo
 * @since 2021/11/3
 */
@Data
public class ListResultHolder<T> {

    /**
     * The codeValue from enum MockerCategory
     *
     * @see MockCategoryType
     */
    private String categoryName;
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    private List<T> record;
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    private List<T> replayResult;
}