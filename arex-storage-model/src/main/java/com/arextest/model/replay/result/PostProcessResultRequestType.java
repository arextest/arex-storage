package com.arextest.model.replay.result;

import lombok.Data;

import java.util.List;

/**
 * @author qzmo
 * @since 2023/09/19
 */
@Data
public class PostProcessResultRequestType {
    private List<ResultCodeGroup> results;
}