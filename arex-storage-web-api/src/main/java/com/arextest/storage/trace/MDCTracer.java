package com.arextest.storage.trace;

import com.arextest.storage.model.enums.MockCategoryType;
import com.arextest.storage.model.mocker.MockItem;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;

/**
 * @author jmo
 * @since 2021/11/5
 */
public final class MDCTracer {
    private static final String CATEGORY = "category";
    private static final String REPLAY_ID = "replayId";
    private static final String RECORD_ID = "recordId";

    private MDCTracer() {

    }

    public static void addTrace(MockCategoryType category, MockItem requestType) {
        addCategory(category);
        if (requestType == null) {
            return;
        }
        addRecordId(requestType.getRecordId());
        addReplayId(requestType.getReplayId());
    }

    private static void put(String name, String value) {
        if (StringUtils.isNotEmpty(value)) {
            MDC.put(name, value);
        }
    }

    public static void addReplayId(String replayId) {
        put(REPLAY_ID, replayId);
    }

    public static void addRecordId(String recordId) {
        put(RECORD_ID, recordId);
    }

    public static void addCategory(MockCategoryType category) {
        if (category != null) {
            put(CATEGORY, category.getDisplayName());
        }
    }

    public static void removeCategory() {
        MDC.remove(CATEGORY);
    }

    public static void clear() {
        MDC.clear();
    }
}