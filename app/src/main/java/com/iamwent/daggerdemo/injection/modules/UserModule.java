package com.iamwent.daggerdemo.injection.modules;

import android.content.Context;

import com.iamwent.daggerdemo.injection.ActivityScope;

import java.util.ArrayList;
import java.util.List;

import dagger.Module;
import dagger.Provides;

/**
 * Created by iamwent on 2016/3/8.
 */
@Module
public class UserModule {
    private static final int COUNT = 10;

    private final Context context;

    public UserModule(Context context) {
        this.context = context;
    }

    @Provides
    @ActivityScope
    Context provideActivityContext() {
        return context;
    }

    @Provides
    @ActivityScope
    List<String> provideUsers() {
        List<String> users = new ArrayList<>(COUNT);

        for (int i = 0; i < COUNT; i++) {
            users.add("item " + i);
        }

        return users;
    }
}
