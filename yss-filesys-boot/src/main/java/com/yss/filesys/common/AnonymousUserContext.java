package com.yss.filesys.common;

public final class AnonymousUserContext {

    public static final String USER_ID = "anonymous";
    public static final String USERNAME = "anonymous";

    private AnonymousUserContext() {
    }

    public static String userId() {
        return USER_ID;
    }

    public static String username() {
        return USERNAME;
    }
}
