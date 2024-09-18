package com.arextest.storage.service.handler.mocker.coverage;

import com.arextest.model.mock.Mocker;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

/**
 * @author: QizhengMo
 * @date: 2024/9/18 19:00
 */
@Service
@ConditionalOnMissingBean(CoverageEventListener.class)
public class DefaultCoverageEventListener implements CoverageEventListener {
    @Override
    public void onNewCaseRecorded(Mocker coverageMocker) {
    }

    @Override
    public void onExistingCaseRecorded(Mocker coverageMocker) {
    }
}
