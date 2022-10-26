package com.arextest.storage.core.repository.impl.redis;


import com.arextest.storage.core.cache.CacheKeyUtils;
import com.arextest.storage.core.repository.RepositoryProvider;
import com.arextest.common.cache.CacheProvider;
import com.arextest.storage.model.enums.MockCategoryType;
import com.arextest.storage.model.mocker.ConfigVersion;
import com.arextest.storage.model.mocker.impl.ConfigVersionMocker;
import com.arextest.storage.model.replay.ReplayCaseRangeRequestType;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;


/**
 * @author jmo
 * @since 2021/11/16
 */
@Component
final class ConfigVersionMockerRepositoryImpl implements RepositoryProvider<ConfigVersionMocker> {
    @Resource
    private CacheProvider cacheProvider;

    @Override
    public MockCategoryType getCategory() {
        return MockCategoryType.CONFIG_VERSION;
    }

    @Override
    public Iterable<ConfigVersionMocker> queryRecordList(String recordId) {
        return null;
    }

    @Override
    public ConfigVersionMocker queryRecord(String recordId) {
        return null;
    }

    @Override
    public Iterable<ConfigVersionMocker> queryReplayResult(String replayResultId) {
        return null;
    }

    @Override
    public Iterable<ConfigVersionMocker> queryByRange(ReplayCaseRangeRequestType rangeRequestType) {
        return null;
    }

    @Override
    public ConfigVersionMocker queryByVersion(ConfigVersion versionRequestType) {
        final ConfigVersionMocker versionMocker = (ConfigVersionMocker) versionRequestType;
        final byte[] key = CacheKeyUtils.merge(versionRequestType.getAppId(), MockCategoryType.CONFIG_VERSION);
        long recordVersion = cacheProvider.incrValue(key);
        versionMocker.setRecordVersion((int) recordVersion);
        return versionMocker;
    }

    @Override
    public int countByRange(ReplayCaseRangeRequestType rangeRequestType) {
        return 0;
    }

    @Override
    public boolean save(ConfigVersionMocker objectValue) {
        return false;
    }

    @Override
    public boolean saveList(List<ConfigVersionMocker> objectValueList) {
        return false;
    }

    @Override
    public boolean saveFixedRecord(ConfigVersionMocker objectValue) {
        return false;
    }
}
