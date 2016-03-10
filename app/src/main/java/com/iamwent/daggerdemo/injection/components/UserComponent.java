package com.iamwent.daggerdemo.injection.components;

import com.iamwent.daggerdemo.injection.ActivityScope;
import com.iamwent.daggerdemo.injection.modules.UserModule;
import com.iamwent.daggerdemo.ui.activity.MainActivity;

import dagger.Component;

/**
 * Created by iamwent on 2016/3/8.
 */
@ActivityScope
@Component(modules = {UserModule.class})
public interface UserComponent {
    void inject(MainActivity activity);
}
