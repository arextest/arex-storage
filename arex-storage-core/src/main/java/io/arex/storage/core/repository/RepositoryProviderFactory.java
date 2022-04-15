package io.arex.storage.core.repository;

import io.arex.storage.model.mocker.MockItem;
import io.arex.storage.model.enums.MockCategoryType;
import lombok.Getter;
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

    @SuppressWarnings("unchecked")
    public <T extends MockItem> RepositoryProvider<T> findProvider(MockCategoryType category) {
        RepositoryProvider<? extends MockItem> repositoryProvider;
        // TODO:use map impl
        for (int i = 0; i < repositoryProviderList.size(); i++) {
            repositoryProvider = repositoryProviderList.get(i);
            if (category == repositoryProvider.getCategory()) {
                return (RepositoryProvider<T>) repositoryProvider;
            }
        }
        return null;
    }
}
