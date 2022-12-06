package com.arextest.storage.mock;

import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker;

import java.util.List;


/**
 * How to fetch mock value for a requested?
 * The order:
 * 1, exact match
 * 2, any fuzzy match
 * 3, use operationName of Mocker as a fallback match,make sure this key as a last one.
 */
public interface MatchKeyBuilder {

    boolean isSupported(MockCategoryType categoryType);

    List<byte[]> build(Mocker instance);
}