package com.arextest.storage.mock;

import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker;

import java.util.List;


public interface MatchKeyBuilder {

    boolean isSupported(MockCategoryType categoryType);

    List<byte[]> build(Mocker instance);
}