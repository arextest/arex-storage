package com.arextest.storage.service.config;

import java.util.List;

/**
 * @author jmo
 * @since 2022/1/22
 */
public interface ConfigurableHandler<T> extends ViewHandler<T> {

    boolean insert(T configuration);

    boolean remove(T configuration);

    boolean insertList(List<T> configurationList);

    boolean removeList(List<T> configurationList);

    boolean update(T configuration);
}
