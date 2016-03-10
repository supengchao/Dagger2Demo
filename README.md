> 假设你已经了解 [**依赖注入**](http://www.devtf.cn/?p=1248) 这一概念，只是在如何使用 Dagger 时遇到了一些困扰，因为 Dagger 其实是一个上手难度颇高的库。我试图通过这篇文章解决如何上手这一问题。

目前 Dagger 有两个分支，一个由 [Square](https://github.com/square/dagger) 维护，一个为 Google 在前者的基础上开出的分支，即 [Dagger 2](https://github.com/google/dagger) 。关于二者的比较，[点击此处](http://www.jcodecraeer.com/a/anzhuokaifa/androidkaifa/2015/0519/2892.html) 。

本文写作过程中参考了不少优秀的 Dagger 文章，列在文章末尾。

**在此一并感谢他们的工作！**

## Dagger

在引入 Dagger 之前，我们需要了解一些基础概念。Dagger 主要分三块：
- **@Inject**：需要注入依赖的地方，Dagger 会构造一个该类的实例并满足它所需要的依赖；
- **@Module**：依赖的提供者，Module 类中的方法专门提供依赖，并用 **@Provides** 注解标记；
- **@Component**：依赖的注入者，是 **@Inject** 和 **@Module** 的桥梁，它从 **@Module** 中获取依赖并注入给 **@Inject**。

对于以上关系，一句话解释就是：**模块（Module）负责提供依赖，组件（Component）负责注入依赖。**

## Sourcecode

## Gradle
1. project/build.gradle
    ```groovy
    buildscript {
        ...
    
        dependencies {
            ...
    
            classpath 'com.neenbedankt.gradle.plugins:android-apt:1.8'
        }
    }
    ```

2. project/app/build.gradle
    ```groovy
    apply plugin: 'com.android.application'
    apply plugin: 'com.neenbedankt.android-apt'
    
    android {
        ...
    }
    
    dependencies {
        ...
    
        //Required by Dagger2
        apt 'com.google.dagger:dagger-compiler:2.0.2'
        compile 'com.google.dagger:dagger:2.0.2'
        // Dagger 2 中会用到 @Generated 注解，而 Android SDK 中没有 javax.anotation.Generated.class，所以在 Android 项目要添加此句
        provided 'org.glassfish:javax.annotation:10.0-b28' 
    }
    ```

## Example
Demo 实现一个简单的 `ListView` 显示字符串列表，源码在 Github ： [DaggerDemo]()

1. 创建 `UserAdapter` 类，并在构造函数前添加 **@Inject** 注解。这样，Dagger 就会在需要获取 `UserAdapter` 对象时，调用这个被标记的构造函数，从而生成一个 `UserAdapter` 对象。
    ```java
    public class UserAdapter extends BaseAdapter {
        private LayoutInflater inflater;
        private List<String> users;
    
        @Inject
        public UserAdapter(Context ctx, List<String> users) {
            this.inflater = LayoutInflater.from(ctx);
            this.users = users;
        }

        ...
    }
    ```
    
    需要注意的是：如果构造函数含有参数，Dagger 会在调用构造对象的时候先去获取这些参数(不然谁来传参？)，所以你要保证它的参数也提供可被 Dagger 调用到的生成函数。Dagger 可调用的对象生成方式有两种：一种是用 **@Inject** 修饰的构造函数，上面就是这种方式。另外一种是用 **@Provides** 修饰的函数，下面会讲到。[参考：Dagger 源码解析](http://a.codekk.com/detail/Android/%E6%89%94%E7%89%A9%E7%BA%BF/Dagger%20%E6%BA%90%E7%A0%81%E8%A7%A3%E6%9E%90)

2. 构建 Module，提供 Context 和 List<String> 依赖，如此， Dagger 生成 UserAdapter 时所需要的依赖就从这里获取。
    ```java
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
    ```

    **@ActivityScope** 是一个自定义的范围注解，作用是允许对象被记录在正确的组件中，当然这些对象的生命周期应该遵循 Activity 的生命周期。
    ```java
    import java.lang.annotation.Retention;
    import static java.lang.annotation.RetentionPolicy.RUNTIME;
    
    import javax.inject.Scope;
    
    @Scope
    @Retention(RUNTIME)
    public @interface ActivityScope {
    
    }
    ```

3. 构建 Component，负责注入依赖
    ```java
    @ActivityScope
    @Component(modules = {UserModule.class})
    public interface UserComponent {
        void inject(MainActivity activity);
    }
    ```

    **注意**：这里必须是真正消耗依赖的类型 `MainActivity`，而不可以写成其父类，比如   `Activity` ，否则会导致注入失败。([参考：使用Dagger 2进行依赖注入](http://codethink.me/2015/08/06/dependency-injection-with-dagger-2/))

4. 完成依赖注入
    ```java
    public class MainActivity extends AppCompatActivity {

        ...
    
        @Inject
        UserAdapter adapter;
    
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            
            ...
    
            // 完成注入
            DaggerUserComponent.builder()
                .userModule(new UserModule(this))
                .build()
                .inject(this);
    
            listView.setAdapter(adapter);
        }
    }
    ```

    **如果找不到 `DaggerUserComponent` 类，你需要先编译一下整个项目。**这是因为 Dagger 是在编译时生成必要的元素，编译时 Dagger 会处理我们的注解，为 @Components 生成实现并命名为 `Dagger$${YouComponentClassName}`，如 `UserComponent` -> `DaggerUserComponent` 。你可以在 `app/build/generated/source/apt` 下找到相关的类。

    实际上，调用 inject 方法最终调用的是以下这样一段代码，更多细节可以查看源码。
    ```java
    @Override
    public void injectMembers(MainActivity instance) {  
      if (instance == null) {
        throw new NullPointerException("Cannot inject members into a null reference");
      }
      supertypeInjector.injectMembers(instance);
      instance.adapter = adapterProvider.get();
    }
    ```

## Dagger too
1. @Inject 和 @Provide 两种依赖生成方式的区别：
    - @Inject 用于注入可实例化的类，@Provides 可用于注入所有类
    - @Inject 可用于修饰属性和构造函数，可用于任何非 Module 类，@Provides 只可用于用于修饰非构造函数，并且该函数必须在某个Module内部
    - @Inject 修饰的函数只能是构造函数，@Provides 修饰的函数必须以 provide 开头

2. Dagger 的其他注解：

    - **@Scope**： Dagger 可以通过自定义注解限定注解作用域，参考前面的 **@ActivityScope**。
    - **@Qualifier**：限定符，当类的类型不足以鉴别一个依赖的时候，我们就可以使用这个注解来区分。例如：在 Android 中，我们会需要不同类型的 Context，所以我们可以定义 @Qualifier 注解 `@ForApplication` 和 `@ForActivity`，这样当注入一个 Context 的时候，我们就可以告诉  Dagger 我们想要哪种类型的 Context。
    - **@Singleton**：单例模式，依赖的对象只会被初始化一次

3. Dagger 的实际应用：本例只是一个上手教程，辅助理解 Dagger 的原理及使用方式，具体的项目应用可以参考 Reference 中第 3 条的 [Avengers 的源码](https://github.com/saulmm/Avengers) 。

## Reference

1. [Dagger 2 Official Documentation](http://google.github.io/dagger/)
2. [Tasting Dagger 2 on Android](http://fernandocejas.com/2015/04/11/tasting-dagger-2-on-android/)，中文：[详解 Dagger 2](http://www.jcodecraeer.com/a/anzhuokaifa/androidkaifa/2015/0519/2892.html)
3. [When the Avengers meet Dagger2, RxJava and Retrofit in a clean way](http://saulmm.github.io/when-Thor-and-Hulk-meet-dagger2-rxjava-1)，中文：[当复仇者联盟遇上 Dragger2、RxJava 和 Retrofit 的巧妙结合](http://www.devtf.cn/?p=565)
4. [使用 Dagger 2 进行依赖注入](http://codethink.me/2015/08/06/dependency-injection-with-dagger-2/)
5. [Dagger 源码解析](http://a.codekk.com/detail/Android/%E6%89%94%E7%89%A9%E7%BA%BF/Dagger%20%E6%BA%90%E7%A0%81%E8%A7%A3%E6%9E%90) (PS: 这是 Dagger 1，但是很有参考价值)
