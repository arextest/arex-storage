package com.arextest.storage.repository;

import com.arextest.storage.model.enums.MockCategoryType;
import com.arextest.storage.model.mocker.MockItem;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author jmo
 * @since 2021/11/11
 */
@Component
public final class RepositoryProviderFactory {
    @Getter
    @Resource
    private List<RepositoryProvider<? extends MockItem>> repositoryProviderList;

    public <T extends MockItem> RepositoryProvider<T> findProvider(MockCategoryType category) {
        return findProvider(ProviderNames.DEFAULT, category);
    }

    @SuppressWarnings("unchecked")
    public <T extends MockItem> RepositoryProvider<T> findProvider(String providerName, MockCategoryType category) {
        RepositoryProvider<? extends MockItem> repositoryProvider;
        // TODO:use map impl
        for (int i = 0; i < repositoryProviderList.size(); i++) {
            repositoryProvider = repositoryProviderList.get(i);
            if (!StringUtils.equals(providerName, repositoryProvider.getProviderName())) {
                continue;
            }
            if (category == repositoryProvider.getCategory()) {
                return (RepositoryProvider<T>) repositoryProvider;
            }
        }
        return null;
    }
}