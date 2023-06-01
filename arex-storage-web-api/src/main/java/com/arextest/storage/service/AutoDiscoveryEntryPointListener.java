package com.arextest.storage.service;

import com.arextest.common.cache.CacheProvider;
import com.arextest.model.mock.Mocker;
import com.arextest.storage.cache.CacheKeyUtils;
import com.arextest.storage.mock.MockResultContext;
import com.arextest.storage.model.dao.ServiceEntity;
import com.arextest.storage.model.dao.ServiceOperationEntity;
import com.arextest.storage.repository.ServiceOperationRepository;
import com.arextest.storage.repository.ServiceRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
public class AutoDiscoveryEntryPointListener implements AgentWorkingListener {
    private final ServiceRepository serviceRepository;
    private final ServiceOperationRepository serviceOperationRepository;
    private final CacheProvider cacheProvider;

    private static final String DASH = "_";
    private static final int SERVICE_TYPE_NORMAL = 4;
    private static final String SERVICE_MAPPINGS_PREFIX = "service_mappings_";
    private static final byte[] EMPTY_BYTE_ARRAY = CacheKeyUtils.toUtf8Bytes(StringUtils.EMPTY);

    public AutoDiscoveryEntryPointListener(ServiceRepository serviceRepository,
            ServiceOperationRepository serviceOperationRepository,
            CacheProvider cacheProvider) {
        this.serviceRepository = serviceRepository;
        this.serviceOperationRepository = serviceOperationRepository;
        this.cacheProvider = cacheProvider;
    }

    private <T extends Mocker> void register(@NotNull T item) {
        String appId = item.getAppId();
        byte[] appServiceKey = CacheKeyUtils.toUtf8Bytes(SERVICE_MAPPINGS_PREFIX + appId);
        if (cacheProvider.get(appServiceKey) == null) {
            ServiceEntity serviceEntity = serviceRepository.queryByAppId(appId);
            if (serviceEntity == null) {
                LOGGER.info("AppId:{} does not have a valid service", appId);
                return;
            } else {
                cacheProvider.put(appServiceKey, CacheKeyUtils.toUtf8Bytes(serviceEntity.getId()));
            }
        }
        String operationName = item.getOperationName();
        String serviceId = CacheKeyUtils.fromUtf8Bytes(cacheProvider.get(appServiceKey));
        byte[] operationKey =
                CacheKeyUtils.toUtf8Bytes(SERVICE_MAPPINGS_PREFIX
                        + serviceId
                        + DASH
                        + item.getOperationName()
                        + DASH
                        + item.getCategoryType().getName());
        if (cacheProvider.get(operationKey) != null) {
            return;
        }
        ServiceOperationEntity operationEntity = new ServiceOperationEntity();
        operationEntity.setAppId(appId);
        operationEntity.setOperationName(operationName);
        operationEntity.setOperationType(item.getCategoryType().getName());
        operationEntity.setOperationTypes(Collections.singletonList(item.getCategoryType().getName()));
        operationEntity.setServiceId(serviceId);
        operationEntity.setStatus(SERVICE_TYPE_NORMAL);
        if (serviceOperationRepository.findAndUpdate(operationEntity)) {
            cacheProvider.put(operationKey, EMPTY_BYTE_ARRAY);
        }
    }

    @Override
    public boolean onRecordSaving(Mocker instance) {
        if (instance.getCategoryType().isEntryPoint()) {
            this.register(instance);
        }
        return false;
    }

    @Override
    public boolean onRecordMocking(Mocker instance, MockResultContext context) {
        return false;
    }


}