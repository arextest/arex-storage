package com.arextest.model.replay.result;

import lombok.Data;

import java.util.List;

@Data
public class ResultCodeGroup {
    private List<CategoryGroup> categoryGroups;
    private int resultCode;

    @Data
    public static class CategoryGroup {
        private String categoryName;
        private List<String> recordIds;
    }
}
