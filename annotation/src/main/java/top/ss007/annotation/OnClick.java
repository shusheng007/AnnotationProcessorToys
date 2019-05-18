package top.ss007.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import androidx.annotation.IdRes;

/**
 * Copyright (C) 2019 shusheng007
 * 完全享有此软件的著作权，违者必究
 *
 * @author shusheng007
 * @version 1.0
 * @modifier
 * @createDate 2019/5/18 9:25
 * @description
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface OnClick {
    @IdRes int value();
}
