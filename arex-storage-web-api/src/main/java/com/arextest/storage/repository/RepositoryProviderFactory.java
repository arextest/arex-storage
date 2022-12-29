package com.arextest.storage.repository;

import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@SuppressWarnings("unchecked")
@Component
public final class RepositoryProviderFactory {
    private final List<RepositoryProvider<? extends Mocker>> repositoryProviderList;
    @Getter
    private final Set<MockCategoryType> categoryTypes;

    public RepositoryProviderFactory(List<RepositoryProvider<? extends Mocker>> repositoryProviderList, Set<MockCategoryType> categoryTypes) {
        this.repositoryProviderList = repositoryProviderList;
        this.categoryTypes = categoryTypes;
    }

    public <T extends Mocker> RepositoryProvider<T> findProvider(String providerName) {
        if (StringUtils.isEmpty(providerName)) {
            providerName = ProviderNames.DEFAULT;
        }
        RepositoryProvider<? extends Mocker> repositoryProvider;
        for (int i = 0; i < repositoryProviderList.size(); i++) {
            repositoryProvider = repositoryProviderList.get(i);
            if (StringUtils.equals(providerName, repositoryProvider.getProviderName())) {
                return (RepositoryProvider<T>) repositoryProvider;
            }
        }
        return null;
    }

    public MockCategoryType findCategory(String categoryName) {
        if (StringUtils.isEmpty(categoryName)) {
            return null;
        }
        for (MockCategoryType categoryType : categoryTypes) {
            if (StringUtils.equals(categoryName, categoryType.getName())) {
                return categoryType;
            }
        }
        return null;
    }

    public <T extends Mocker> RepositoryProvider<T> defaultProvider() {
        return findProvider(ProviderNames.DEFAULT);
    }
}