package com.qinghe.life.enums;

public enum CouponStatus {
    ENABLED, DISABLED;

    public static boolean isValid(String value) {
        for (CouponStatus status : values()) {
            if (status.name().equals(value)) {
                return true;
            }
        }
        return false;
    }
}
