package com.arextest.storage.service.config;

import java.util.List;

import com.arextest.storage.repository.ConfigRepositoryProvider;
import org.apache.commons.collections4.CollectionUtils;

/**
 * @author jmo
 * @since 2022/2/4
 */
public abstract class AbstractConfigurableHandler<T> implements ConfigurableHandler<T> {
    protected final ConfigRepositoryProvider<T> repositoryProvider;

    protected AbstractConfigurableHandler(ConfigRepositoryProvider<T> repositoryProvider) {
        this.repositoryProvider = repositoryProvider;
    }

    @Override
    public List<T> useResultAsList(String appId) {
        List<T> sourceList = repositoryProvider.listBy(appId);
        if (CollectionUtils.isEmpty(sourceList)) {
            return createFromGlobalDefault(appId);
        }
        if (this.shouldMergeGlobalDefault()) {
            for (T source : sourceList) {
                this.mergeGlobalDefaultSettings(source);
            }
        }
        return sourceList;
    }

    @Override
    public List<T> useResultAsList() {
        return repositoryProvider.list();
    }

    @Override
    public boolean insertList(List<T> configurationList) {
        return repositoryProvider.insertList(configurationList);
    }

    @Override
    public final boolean removeList(List<T> configurationList) {
        return repositoryProvider.removeList(configurationList);
    }

    @Override
    public boolean insert(T configuration) {
        return repositoryProvider.insert(configuration);
    }

    @Override
    public boolean remove(T configuration) {
        return repositoryProvider.remove(configuration);
    }

    @Override
    public boolean update(T configuration) {
        return repositoryProvider.update(configuration);
    }

    protected List<T> createFromGlobalDefault(String appId) {
        return null;
    }

    protected boolean shouldMergeGlobalDefault() {
        return false;
    }

    protected void mergeGlobalDefaultSettings(T source) {

    }
}
