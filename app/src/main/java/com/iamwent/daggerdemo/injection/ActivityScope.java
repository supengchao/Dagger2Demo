package com.iamwent.daggerdemo.injection;

import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import javax.inject.Scope;

/**
 * Created by iamwent on 2016/3/8.
 */

@Scope
@Retention(RUNTIME)
public @interface ActivityScope {

}
