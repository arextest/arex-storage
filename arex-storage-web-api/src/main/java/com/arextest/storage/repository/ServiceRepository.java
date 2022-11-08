package com.arextest.storage.repository;

import com.arextest.storage.model.dao.ServiceEntity;

/**
 * @author b_yu
 * @since 2022/8/25
 */
public interface ServiceRepository extends Repository {
    ServiceEntity queryByAppId(String appId);
}