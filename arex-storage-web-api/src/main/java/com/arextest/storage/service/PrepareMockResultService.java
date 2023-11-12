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

  boolean preload(MockCategoryType category, String recordId) {
    // try again load by defaultProvider and pinnedProvider
    return this.preload(providerFactory.getRepositoryProviderList(), category, recordId);
  }

  private boolean preload(RepositoryProvider<? extends Mocker> repositoryReader,
      MockCategoryType categoryType, String recordId) {
    if (repositoryReader == null) {
      return false;
    }
    if (mockResultProvider.recordResultCount(categoryType, recordId) > 0) {
      LOGGER.warn("skip preload cache for category:{},record id:{}", categoryType, recordId);
      return true;
    }
    Iterable<? extends Mocker> iterable;
    iterable = repositoryReader.queryRecordList(categoryType, recordId);
    if (iterable == null) {
      return false;
    }
    return mockResultProvider.putRecordResult(categoryType, recordId, iterable);
  }

  private boolean preload(List<RepositoryProvider<? extends Mocker>> repositoryReaderList,
      MockCategoryType categoryType, String recordId) {
    for (RepositoryProvider<? extends Mocker> repositoryReader : repositoryReaderList) {
      if (this.preload(repositoryReader, categoryType, recordId)) {
        return true;
      }
    }
    return false;
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

  public boolean removeAllResultCache(String resultId) {
    boolean result = false;
    for (MockCategoryType categoryType : providerFactory.getCategoryTypes()) {
      result = removeResult(categoryType, resultId);
    }
    return result;
  }

  public boolean removeResult(MockCategoryType category, String resultId) {
    return mockResultProvider.removeReplayResult(category, resultId);
  }
}