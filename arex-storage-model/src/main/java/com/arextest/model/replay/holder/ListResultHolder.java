package com.arextest.model.replay.holder;

import com.arextest.model.mock.MockCategoryType;
import lombok.Data;

import java.util.List;

/**
 * @author jmo
 * @since 2021/11/3
 */
@Data
public class ListResultHolder {

    /**
     * The codeValue from enum MockerCategory
     *
     * @see MockCategoryType
     */
    private MockCategoryType categoryType;
    private List<String> record;
    private List<String> replayResult;
}