package com.arextest.storage.repository;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;

/**
 * @author jmo
 * @since 2022/1/25
 */
public interface ConfigRepositoryProvider<T> extends Repository {
    List<T> list();

    List<T> listBy(String appId);

    boolean update(T configuration);

    boolean remove(T configuration);

    boolean insert(T configuration);

    default boolean insertList(List<T> configurationList) {
        if (CollectionUtils.isEmpty(configurationList)) {
            return false;
        }
        for (T configuration : configurationList) {
            this.insert(configuration);
        }
        return true;
    }

    default boolean removeList(List<T> configurationList) {
        if (CollectionUtils.isEmpty(configurationList)) {
            return false;
        }
        for (T configuration : configurationList) {
            this.remove(configuration);
        }
        return true;
    }

    default boolean removeByAppId(String appId) {
        return false;
    }

    default long count(String appId) {
        return 0;
    }

}
