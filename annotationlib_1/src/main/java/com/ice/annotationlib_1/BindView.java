package com.ice.annotationlib_1;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 运行时注解BindingView的init会使用反射，有一些性能损耗
 * */
@Retention(RetentionPolicy.RUNTIME)
public @interface BindView {
    int value();
}
