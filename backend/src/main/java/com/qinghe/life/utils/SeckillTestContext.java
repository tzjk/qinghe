package com.qinghe.life.utils;

/** Carries a local-profile test run only while its HTTP request is handled. */
public final class SeckillTestContext {
    private static final ThreadLocal<String> RUN = new ThreadLocal<String>();
    private SeckillTestContext() { }
    public static void setTestRunId(String value) { RUN.set(value); }
    public static String getTestRunId() { return RUN.get(); }
    public static void clear() { RUN.remove(); }
}
