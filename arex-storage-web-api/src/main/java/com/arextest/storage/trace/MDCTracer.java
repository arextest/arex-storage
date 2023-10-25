package com.arextest.storage.trace;

import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker;
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
    private static final String APP_TYPE = "app-type";
    private static final String AREX_STORAGE = "arex-storage";

    private MDCTracer() {

    }

    public static void addTrace(MockCategoryType category, Mocker requestType) {
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

    public static void addAppType() {
        MDC.put(APP_TYPE, AREX_STORAGE);
    }

    public static void addReplayId(String replayId) {
        addAppType();
        put(REPLAY_ID, replayId);
    }

    public static void addRecordId(String recordId) {
        addAppType();
        put(RECORD_ID, recordId);
    }

    public static void addCategory(MockCategoryType category) {
        addAppType();
        if (category != null) {
            put(CATEGORY, category.getName());
        }
    }

    public static void addAppId(String appId) {
        addAppType();
        put("appId", appId);
    }

    public static void removeCategory() {
        MDC.remove(CATEGORY);
    }

    public static void clear() {
        MDC.clear();
    }
}