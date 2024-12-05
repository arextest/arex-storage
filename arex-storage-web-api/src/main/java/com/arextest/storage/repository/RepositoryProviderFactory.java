package com.arextest.storage.repository;

import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker;
import com.arextest.storage.model.Constants;
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

  private final List<RepositoryProvider<? extends Mocker>> repositoryProviderList;
  @Getter
  private final Set<MockCategoryType> categoryTypes;
  private final Map<String, MockCategoryType> categoryTypeMap;
  @Getter
  private final Set<MockCategoryType> entryCategoryTypes;

  public RepositoryProviderFactory(
      List<RepositoryProvider<? extends Mocker>> repositoryProviderList,
      Set<MockCategoryType> categoryTypes) {
    this.repositoryProviderList = repositoryProviderList;
    this.categoryTypes = categoryTypes;
    this.categoryTypeMap = categoryTypes.stream()
        .collect(Collectors.toMap(MockCategoryType::getName, Function.identity()));
    this.entryCategoryTypes =  categoryTypes.stream().filter(MockCategoryType::isEntryPoint)
        .collect(Collectors.toSet());
  }

  public List<RepositoryProvider<? extends Mocker>> getRepositoryProviderList() {
    return getRepositoryProviderList(Constants.CLAZZ_NAME_AREX_MOCKER);
  }

  public List<RepositoryProvider<? extends Mocker>> getRepositoryProviderList(String clazzName) {
    return repositoryProviderList.stream()
        .filter(provider -> provider != null && StringUtils.equals(clazzName, provider.getMockerType()))
        .collect(Collectors.toList());
  }

  public <T extends Mocker> RepositoryProvider<T> findProvider(String providerName) {
    return findProvider(providerName, Constants.CLAZZ_NAME_AREX_MOCKER);
  }

  public <T extends Mocker> RepositoryProvider<T> findProvider(String providerName, String clazzName) {
    if (StringUtils.isEmpty(providerName)) {
      providerName = ProviderNames.DEFAULT;
    }
    RepositoryProvider<? extends Mocker> repositoryProvider;
    for (int i = 0; i < repositoryProviderList.size(); i++) {
      repositoryProvider = repositoryProviderList.get(i);
      if (StringUtils.equals(providerName, repositoryProvider.getProviderName()) &&
            StringUtils.equals(clazzName, repositoryProvider.getMockerType())) {
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

    if (MockCategoryType.RECORDING_SCENE.getName().equals(categoryName)) {
      return MockCategoryType.RECORDING_SCENE;
    }

    if (MockCategoryType.REPLAY_SCENE.getName().equals(categoryName)) {
      return MockCategoryType.REPLAY_SCENE;
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
