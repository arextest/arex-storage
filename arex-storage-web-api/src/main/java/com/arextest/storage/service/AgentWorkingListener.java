package com.arextest.storage.service;

import com.arextest.model.mock.Mocker;
import com.arextest.storage.mock.MockResultContext;

/**
 * A listener that is informed about events that occur during recording &amp; replaying process.
 *
 * @author mok
 * @since 2022/11/24
 */
public interface AgentWorkingListener {
    void onRecordSaving(Mocker instance);

    void onRecordMocking(Mocker instance, MockResultContext context);
}