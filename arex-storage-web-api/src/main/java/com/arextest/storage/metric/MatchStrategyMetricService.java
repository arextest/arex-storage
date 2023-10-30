package com.arextest.storage.metric;

import com.arextest.model.mock.AREXMocker;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * created by xinyuan_wang on 2023/09/29
 */
@Slf4j
public class MatchStrategyMetricService {
    private static final String SEARCH_STRATEGY_NAME = "search.strategy";
    private static final String CLIENT_APP_ID = "clientAppId";
    private static final String MATCH_STRATEGY = "matchStrategy";
    private static final String CATEGORY = "category";
    public final List<MetricListener> metricListeners;

    public MatchStrategyMetricService(List<MetricListener> metricListeners) {
        this.metricListeners = metricListeners;
    }

    public void recordMatchingCount(String matchStrategy, AREXMocker item) {
        if (CollectionUtils.isEmpty(metricListeners)) {
            return;
        }

        Map<String, String> tags = new HashMap<>(5);
        tags.put(CLIENT_APP_ID, item.getAppId());
        tags.put(MATCH_STRATEGY, matchStrategy);
        tags.put(CATEGORY, item.getCategoryType().getName());

        for (MetricListener metricListener : metricListeners) {
            metricListener.recordMatchingCount(SEARCH_STRATEGY_NAME, tags);
        }
    }

}
