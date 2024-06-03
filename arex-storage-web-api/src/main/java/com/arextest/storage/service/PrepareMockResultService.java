package com.arextest.storage.service;

import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker;
import com.arextest.storage.mock.MockResultProvider;
import com.arextest.storage.repository.RepositoryProvider;
import com.arextest.storage.repository.RepositoryProviderFactory;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * As for improve performance goal, The schedule send replay case before,should be call prepare
 * action,touch all dependency to cached. After compared,remove all cached source or auto expired.
 */
@Slf4j
@Service
public final class PrepareMockResultService {

  private final RepositoryProviderFactory providerFactory;
  private final MockResultProvider mockResultProvider;

  public PrepareMockResultService(RepositoryProviderFactory providerFactory,
      MockResultProvider mockResultProvider) {
    this.providerFactory = providerFactory;
    this.mockResultProvider = mockResultProvider;
  }

  /**
   * preload the record data of the all category type to redis by record id
   * @param sourceProvider
   * @param recordId
   * @return
   */
  public boolean preloadAll(String sourceProvider, String recordId) {
    RepositoryProvider<? extends Mocker> repositoryProvider = providerFactory.findProvider(
        sourceProvider);
    if (repositoryProvider == null) {
      return false;
    }
    boolean result = false;
    for (MockCategoryType categoryType : providerFactory.getCategoryTypes()) {
      boolean curResult = preload(repositoryProvider, categoryType, recordId);
      result = curResult || result;
      LOGGER.info("preload cache result:{},category:{},record id:{}", curResult, categoryType,
          recordId);
    }
    return result;
  }

  /**
   * preload the record data of single category type to redis by record id
   *
   * @param category
   * @param recordId
   * @return
   */
  public boolean preload(MockCategoryType category, String recordId) {
    // try again load by defaultProvider and pinnedProvider
    List<RepositoryProvider<? extends Mocker>> repositoryReaderList = providerFactory.getRepositoryProviderList();
    for (RepositoryProvider<? extends Mocker> repositoryReader : repositoryReaderList) {
      if (this.preload(repositoryReader, category, recordId)) {
        return true;
      }
    }
    return false;
  }

  private boolean preload(RepositoryProvider<? extends Mocker> repositoryReader,
      MockCategoryType categoryType, String recordId) {
    if (repositoryReader == null) {
      return false;
    }
    int resultCount = mockResultProvider.recordResultCount(categoryType, recordId);
    if (resultCount > 0) {
      LOGGER.info("preload cache for category:{},record id:{},count:{}", categoryType, recordId, resultCount);
    }
    Iterable<? extends Mocker> iterable;
    iterable = repositoryReader.queryRecordList(categoryType, recordId);
    if (iterable == null) {
      return false;
    }
    return mockResultProvider.putRecordResult(categoryType, recordId, iterable);
  }

  public boolean removeAllRecordCache(String recordId, String sourceProvider) {
    RepositoryProvider<? extends Mocker> repositoryProvider = providerFactory.findProvider(
        sourceProvider);
    if (repositoryProvider == null) {
      return false;
    }
    boolean result = false;
    for (MockCategoryType categoryType : providerFactory.getCategoryTypes()) {
      result = removeRecord(repositoryProvider, categoryType, recordId);
    }
    return result;
  }

  public boolean removeRecord(RepositoryProvider<? extends Mocker> repositoryReader,
      MockCategoryType category, String recordId) {
    if (repositoryReader == null) {
      return false;
    }
    if (mockResultProvider.recordResultCount(category, recordId) <= 0) {
      LOGGER.warn("skip remove cache for category:{},record id:{}", category, recordId);
      return true;
    }
    Iterable<? extends Mocker> iterable;
    iterable = repositoryReader.queryRecordList(category, recordId);
    if (iterable == null) {
      return false;
    }
    return mockResultProvider.removeRecordResult(category, recordId, iterable);
  }

  public boolean removeRecord(String sourceProvider, MockCategoryType category, String recordId) {
    RepositoryProvider<? extends Mocker> repositoryProvider = providerFactory.findProvider(
        sourceProvider);
    if (repositoryProvider == null) {
      return false;
    }
    return removeRecord(repositoryProvider, category, recordId);
  }
}