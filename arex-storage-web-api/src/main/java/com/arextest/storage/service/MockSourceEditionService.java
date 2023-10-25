package com.arextest.storage.service;

import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker;
import com.arextest.storage.repository.RepositoryProvider;
import com.arextest.storage.repository.RepositoryProviderFactory;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MockSourceEditionService {

  private final RepositoryProviderFactory providerFactory;

  public MockSourceEditionService(RepositoryProviderFactory providerFactory,
      Set<MockCategoryType> entryPointTypes) {
    this.providerFactory = providerFactory;
  }

  public <T extends Mocker> boolean add(String providerName, T item) {
    RepositoryProvider<T> repositoryWriter = providerFactory.findProvider(providerName);
    return repositoryWriter != null && repositoryWriter.save(item);
  }

  public <T extends Mocker> boolean update(String providerName, T item) {
    RepositoryProvider<T> repositoryWriter = providerFactory.findProvider(providerName);
    item.setUpdateTime(System.currentTimeMillis());
    return repositoryWriter != null && repositoryWriter.update(item);
  }

  public boolean removeAll(String providerName, String recordId) {
    if (StringUtils.isBlank(recordId)) {
      LOGGER.warn("The recordId is empty");
      return false;
    }
    RepositoryProvider<?> repositoryWriter = providerFactory.findProvider(providerName);
    if (repositoryWriter == null) {
      LOGGER.warn("Could not found provider for {}", providerName);
      return false;
    }
    Set<MockCategoryType> categoryTypes = providerFactory.getCategoryTypes();
    for (MockCategoryType categoryType : categoryTypes) {
      repositoryWriter.removeBy(categoryType, recordId);
    }
    return true;
  }

  public boolean removeAllByAppId(String providerName, String appId) {
    if (StringUtils.isBlank(appId)) {
      LOGGER.warn("The appId is empty");
      return false;
    }
    RepositoryProvider<?> repositoryWriter = providerFactory.findProvider(providerName);
    if (repositoryWriter == null) {
      LOGGER.warn("Could not found provider for {}", providerName);
      return false;
    }
    Set<MockCategoryType> categoryTypes = providerFactory.getCategoryTypes();
    for (MockCategoryType categoryType : categoryTypes) {
      repositoryWriter.removeByAppId(categoryType, appId);
    }
    return true;
  }

  public boolean removeAllByOperationNameAndAppId(String providerName, String operationName,
      String appId) {
    RepositoryProvider<?> repositoryWriter = providerFactory.findProvider(providerName);
    if (StringUtils.isBlank(appId)) {
      LOGGER.warn("The appId is empty");
      return false;
    }
    if (repositoryWriter == null) {
      LOGGER.warn("Could not found provider for {}", providerName);
      return false;
    }
    Set<MockCategoryType> categoryTypes = providerFactory.getCategoryTypes();
    for (MockCategoryType categoryType : categoryTypes) {
      repositoryWriter.removeByOperationNameAndAppId(categoryType, operationName, appId);
    }
    return true;
  }

  public boolean remove(String providerName, String categoryName, String recordId) {
    try {
      RepositoryProvider<?> repositoryWriter = providerFactory.findProvider(providerName);
      if (repositoryWriter == null) {
        LOGGER.warn("Could not found provider for {}", providerName);
        return false;
      }
      MockCategoryType categoryType = providerFactory.findCategory(categoryName);
      if (categoryType == null) {
        LOGGER.warn(
            "Could not found category for {}, did you customize a new category? try register it" +
                " in " +
                "config file",
            categoryName);
        return false;
      }
      repositoryWriter.removeBy(categoryType, recordId);
    } catch (Throwable throwable) {
      LOGGER.error("remove record error:{} from {} for category:{} at recordId:{}",
          throwable.getMessage(),
          providerName, categoryName,
          recordId,
          throwable);
      return false;
    }
    return true;
  }

  public int copyTo(String srcProviderName, String srcRecordId, String targetProviderName,
      String targetRecordId) {
    int count = 0;
    if (StringUtils.equals(srcProviderName, targetProviderName)) {
      return count;
    }
    RepositoryProvider<Mocker> srcProvider = providerFactory.findProvider(srcProviderName);
    RepositoryProvider<Mocker> targetProvider = providerFactory.findProvider(targetProviderName);
    if (srcProvider == null || targetProvider == null) {
      LOGGER.warn("could not found provider for {} or {}", srcProvider, targetProvider);
      return count;
    }
    Iterable<Mocker> srcItemIterable;
    Set<MockCategoryType> categoryTypes = providerFactory.getCategoryTypes();
    for (MockCategoryType categoryType : categoryTypes) {
      srcItemIterable = srcProvider.queryRecordList(categoryType, srcRecordId);
      if (srcItemIterable == null) {
        continue;
      }
      List<Mocker> targetList = createTargetList(srcItemIterable, targetRecordId);
      if (CollectionUtils.isNotEmpty(targetList)) {
        if (targetProvider.saveList(targetList)) {
          count += targetList.size();
        }
      }
    }
    return count;
  }

  public boolean moveTo(String srcProviderName, String srcRecordId, String targetProviderName) {
    copyTo(srcProviderName, srcRecordId, targetProviderName, srcRecordId);
    removeByRecordId(srcProviderName, srcRecordId);
    return true;
  }

  public boolean removeByRecordId(String providerName, String recordId) {
    RepositoryProvider<?> repositoryWriter = providerFactory.findProvider(providerName);
    if (repositoryWriter == null) {
      LOGGER.warn("Could not found provider for {}", providerName);
      return false;
    }
    for (MockCategoryType categoryType : providerFactory.getCategoryTypes()) {
      repositoryWriter.removeBy(categoryType, recordId);
    }
    return true;
  }

  private List<Mocker> createTargetList(Iterable<Mocker> srcItemIterable, String targetRecordId) {
    Iterator<Mocker> valueIterator = srcItemIterable.iterator();
    List<Mocker> targetList = null;
    long now = System.currentTimeMillis();
    while (valueIterator.hasNext()) {
      if (targetList == null) {
        targetList = new LinkedList<>();
      }
      Mocker value = valueIterator.next();
      value.setRecordId(targetRecordId);
      value.setId(null);
      value.setCreationTime(now);
      targetList.add(value);
    }
    return targetList;
  }
}