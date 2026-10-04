package com.stockai.data.feed;

/**
 * Supported time frame intervals.
 */
public enum AngelOneTimeFrameInterval {
    ONE_DAY("ONE_DAY");

    private final String value;

    AngelOneTimeFrameInterval(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
