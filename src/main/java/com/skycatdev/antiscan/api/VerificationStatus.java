package com.skycatdev.antiscan.api;

import net.minecraft.util.StringRepresentable;

public enum VerificationStatus implements StringRepresentable {
    FAIL(0, "fail"),
    PASS(-1, "pass"),
    SUCCEED(1, "succeed");

    private final int priority;
    private final String name;

    VerificationStatus(int priority, String name) {
        this.priority = priority;
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public boolean isPrioritizedOver(VerificationStatus other) {
        return priority > other.priority;
    }

    public VerificationStatus chooseHigher(VerificationStatus other) {
        return highestPriority(this, other);
    }

    public static VerificationStatus highestPriority(VerificationStatus a, VerificationStatus b) {
        return a.priority > b.priority ? a : b;
    }

}
