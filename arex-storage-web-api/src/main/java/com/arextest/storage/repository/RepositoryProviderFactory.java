package com.arextest.storage.repository;

import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@SuppressWarnings("unchecked")
@Component
public final class RepositoryProviderFactory {

  @Getter
  private final List<RepositoryProvider<? extends Mocker>> repositoryProviderList;
  @Getter
  private final Set<MockCategoryType> categoryTypes;
  private final Map<String, MockCategoryType> categoryTypeMap;

  public RepositoryProviderFactory(
      List<RepositoryProvider<? extends Mocker>> repositoryProviderList,
      Set<MockCategoryType> categoryTypes) {
    this.repositoryProviderList = repositoryProviderList;
    this.categoryTypes = categoryTypes;
    this.categoryTypeMap = categoryTypes.stream()
        .collect(Collectors.toMap(MockCategoryType::getName, Function.identity()));
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

    if (MockCategoryType.COVERAGE.getName().equals(categoryName)) {
      return MockCategoryType.COVERAGE;
    }

    return categoryTypeMap.get(categoryName);
  }

  public Set<MockCategoryType> getCategoryTypesByName(String[] categoryList) {
    if (ArrayUtils.isEmpty(categoryList)) {
      return categoryTypes;
    }

    return Arrays.stream(categoryList)
        .map(this::findCategory)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }

  public <T extends Mocker> RepositoryProvider<T> defaultProvider() {
    return findProvider(ProviderNames.DEFAULT);
  }
}
