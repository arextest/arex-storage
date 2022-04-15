package io.arex.storage.core.repository;

import io.arex.storage.model.mocker.MockItem;
import io.arex.storage.model.enums.MockCategoryType;

import javax.validation.constraints.NotNull;

/**
 * @author jmo
 * @since 2021/11/8
 */
public interface RepositoryProvider<T extends MockItem> extends RepositoryReader<T>, RepositoryWriter<T> {
    @NotNull
    MockCategoryType getCategory();
}
