package com.arextest.storage.mock;

import lombok.Getter;
import lombok.Setter;

public class MockResultContext {
    @Setter
    @Getter
    private boolean lastOfResult = false;
    @Getter
    private final MockStrategy mockStrategy;

    public MockResultContext(MockStrategy mockStrategy) {
        this.mockStrategy = mockStrategy;
    }
}