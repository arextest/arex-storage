package com.arextest.storage.core.repository;

import com.arextest.storage.model.dao.ServiceOperationEntity;

/**
 * @author b_yu
 * @since 2022/8/25
 */
public interface ServiceOperationRepository extends Repository {
    boolean findAndUpdate(ServiceOperationEntity entity);
}
