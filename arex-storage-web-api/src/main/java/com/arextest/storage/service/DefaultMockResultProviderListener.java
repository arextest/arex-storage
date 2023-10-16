package com.arextest.storage.service;

import com.arextest.model.mock.Mocker;

/**
 * A listener that is informed about events that occur during mock result.
 *
 * created by xinyuan_wang on 2023/10/16
 */
public interface DefaultMockResultProviderListener {

    /**
     * build key with record operation
     */
    boolean buildRecordOperationKey(Mocker instance);
}