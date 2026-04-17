package com.yss.filesys.storage.plugin.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface StoragePlugin {

    String identifier();

    String name();

    String description() default "";

    String icon() default "icon-storage";

    String link() default "";

    boolean isDefault() default false;

    String configSchema() default "";
}
