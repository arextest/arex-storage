package com.arextest.storage.service.config;

import java.util.List;

/**
 * @author jmo
 * @since 2022/1/22
 */
public interface ViewHandler<T> {
    /**
     * show all exists items
     *
     * @return list all
     */
    default List<T> useResultAsList() {
        return null;
    }

    /**
     * after configured,we apply the result as a single value return.
     *
     * @param appId the value
     * @return config result
     */
    default T useResult(String appId) {
        List<T> useResultList = this.useResultAsList(appId);
        if (useResultList != null && !useResultList.isEmpty()) {
            return useResultList.get(0);
        }
        return null;
    }
    /**
     * after configured,we apply the result as list return.
     *
     * @param appId the appId not null
     * @return config result
     */
    List<T> useResultAsList(String appId);
}
