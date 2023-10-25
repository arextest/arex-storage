package com.arextest.storage.mock;

import lombok.Getter;
import lombok.Setter;

public class MockResultContext {
    @Getter
    private final MockResultMatchStrategy mockStrategy;
    @Setter
    @Getter
    private boolean lastOfResult = false;
    @Setter
    @Getter
    private byte[] valueRefKey;

    public MockResultContext(MockResultMatchStrategy mockStrategy) {
        this.mockStrategy = mockStrategy;
    }
}