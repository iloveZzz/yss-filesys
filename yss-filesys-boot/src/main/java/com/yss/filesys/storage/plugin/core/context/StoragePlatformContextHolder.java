package com.yss.filesys.storage.plugin.core.context;

public final class StoragePlatformContextHolder {

    private static final ThreadLocal<StoragePlatformContext> CONTEXT = new ThreadLocal<>();

    private StoragePlatformContextHolder() {
    }

    public static void setContext(StoragePlatformContext context) {
        CONTEXT.set(context);
    }

    public static String getConfigId() {
        StoragePlatformContext context = CONTEXT.get();
        return context == null ? null : context.configId();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
