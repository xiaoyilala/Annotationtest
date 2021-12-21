[toc]
# 一、本文阅读提示
本文从基础的运行期注解用法开始，逐步演进到编译期注解的用法，让你真正明白编译期注解到底应该在什么场景下使用，怎么用，用了有哪些好处。

# 二、运行期注解
Android开发都写过很多行findViewById ，手写起来很麻烦，没有一丁点乐趣。我们首先用运行期注解来解决这个问题。  
1. 定义一个lib module,包含自定义的注解类
```
// BindView.java
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface BindView {
    int value();
}
```
2. 让主工程依赖我们这个lib，可以在主工程中使用这个注解，虽然现在没有起到什么作用

```
public class MainActivity extends AppCompatActivity {

    @BindView(R.id.tv)
    TextView tv;
```

3. 因为lib不能依赖于主工程，那我们拿不到MainActivity，也不知道有多少个field，这里可以通过反射来获得

```
import android.app.Activity;
import java.lang.reflect.Field;

public class BindingView {
    public static void init(Activity activity){
        Field[] fields = activity.getClass().getDeclaredFields();
        for(Field field:fields){
            BindView annotation = field.getAnnotation(BindView.class);
            if(annotation!=null){
                int viewId = annotation.value();
                field.setAccessible(true);
                try {
                    field.set(activity, activity.findViewById(viewId));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
```
4. 最后在主工程中使用

```
public class MainActivity extends AppCompatActivity {

    @BindView(R.id.tv)
    TextView tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BindingView.init(this);
        tv.setText("运行时注解")
```
5. 从上面反射的代码可以看出，反射是在for循环中的，会随着界面复杂程度的提高而逐渐影响性能

# 三、编译期注解
为了解决上面的问题，可以使用编译期注解。我们可以使用一个辅助的类来解决上面for循环中的反射。
1. 在主工程中新建一个MainActivityViewBinding的类。

```
public class MainActivityViewBinding {
  public MainActivityViewBinding(MainActivity activity) {
    activity.tv=activity.findViewById(R.id.tv);;
  }
}
```
2. 在BindingView中来调用这个类

```
public class BindingView {
 
    public static void init(Activity activity) {
        try {
            Class bindingClass = Class.forName(activity.getClass().getCanonicalName() + "ViewBinding");
            Constructor constructor = bindingClass.getDeclaredConstructor(activity.getClass());
            constructor.newInstance(activity);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
```
**虽然这里也用到了反射，但是它只有一次，和上面在for循环中使用完全不一样**
3. 现在在虽然解决了反射的问题，但那个辅助类是我们手写的，那如何解决这个问题呢？这里就引出了apt，也就是编译期注解的核心部分。
4. 创建Java lib，注意是**Java lib**不是android lib，然后在主工程中引入。  
annotationProcessor project(path: ':lib_processor')
5. 创建一个注解处理类

```
public class BindingProcessor extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        return false;
    }
}
```
6. 在工程目录main下创建resources/META-INF/services/javax.annotation.processing.Processor文件，文件中写入我们刚刚创建的注解处理类

```
com.xxx.lib_processor.BindingProcessor
```
7. 再创建一个Java lib，只放BindView，让我们的lib_processor和主工程 都依赖这个注解工程，这里要改一下代码

```
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.FIELD)
public @interface BindView {
    int value();
}
```

```
public class BindingProcessor extends AbstractProcessor {
 
    Messager messager;
    Filer filer;
    Elements elementUtils;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        //输出一些重要的日志使用
        messager = processingEnv.getMessager();
        //返回用于创建新源文件、类或辅助文件的文件管理器。
        //理解成最终我们写java文件 要用到的重要 输出参数即可
        filer = processingEnv.getFiler();
        //一些方便的utils方法
        elementUtils = processingEnv.getElementUtils();
        //Diagnostic.Kind.ERROR 是可以让编译失败的 一些重要的参数校验可以用这个来提示用户你哪里写的不对
        messager.printMessage(Diagnostic.Kind.NOTE, "BindingProcessor init");
        super.init(processingEnv);
    }
 
    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        return false;
    }
 
    //要支持哪些注解
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(BindView.class.getCanonicalName());
    }
}
```
8. 现在我们核心的问题是要动态的生成MainActivityViewBinding这个类，有下面几个问题摆在我们面前：
- 首先要获取哪些类使用了我们的BindView注解；
- 获取这些类中使用了BindView注解的field以及他们对应的值；
- 拿到这些类的类名称以便我们生成诸如MainActivityViewBinding这样的类名；
- 拿到这些类的包名，因为我们生成的类要和注解所属的类属于同一个package 才不会出现field 访问权限的问题；
9. 用字符串拼接的方式来实现

```
@Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // key 就是使用注解的class的类名 element就是使用注解本身的元素 一个class 可以有多个使用注解的field
        Map<String, List<Element>> fieldMap = new HashMap<>();
        // 这里 获取到 所有使用了 BindView 注解的 element
        for(Element element: roundEnv.getElementsAnnotatedWith(BindView.class)){

//            TypeMirror typeMirror = element.asType();
//            TypeName typeName = ClassName.get(typeMirror);

            //取到 这个注解所属的class的Name
            String className = element.getEnclosingElement().getSimpleName().toString();
            //生成的类 要和 注解的类 同属一个package 所以还要取 package的名称
            String packageName = elementUtils.getPackageOf(element).getQualifiedName().toString();
            String key = packageName+"."+className;
            //取到值以后 判断map中 有没有 如果没有就直接put 有的话 就直接在这个value中增加一个element
            if(fieldMap.get(key)!=null){
                List<Element> elementList = fieldMap.get(key);
                elementList.add(element);
            }else{
                ArrayList<Element> elements = new ArrayList<>();
                elements.add(element);
                fieldMap.put(key, elements);
            }
        }

        //遍历map，开始生成辅助类
        for(Map.Entry<String, List<Element>> entry:fieldMap.entrySet()){
            try {
                generateCodeByStringBuffer(entry.getValue());
//                generateCodeByJavapoetActivity(entry.getKey(), entry.getValue());
//                generateCodeByJavapoet(entry.getKey(), entry.getValue());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
    
    
    private void generateCodeByStringBuffer(List<Element> elements) throws IOException {
        String className = elements.get(0).getEnclosingElement().getSimpleName().toString();
        //生成的类 要和 注解的类 同属一个package 所以还要取 package的名称
        String packageName = elementUtils.getPackageOf(elements.get(0)).getQualifiedName().toString();
        StringBuffer sb = new StringBuffer();
        // 每个java类 的开头都是package
        sb.append("package ");
        sb.append(packageName);
        sb.append(";\n");

        // public class XXXActivityViewBinding {
        final String classDefine = "public class " + className + "ViewBinding { \n";
        sb.append(classDefine);

        //定义构造函数的开头
        String constructorName = "public " + className + "ViewBinding(" + className + " activity){ \n";
        sb.append(constructorName);

        //遍历所有element 生成诸如 activity.tv=activity.findViewById(R.id.xxx) 之类的语句
        for(Element e:elements) {
            sb.append("activity." + e.getSimpleName() + "=activity.findViewById(" + e.getAnnotation(BindView.class).value() + ");\n");
        }
        sb.append("\n}");
        sb.append("\n}");

        //文件内容确定以后 直接生成即可
        JavaFileObject sourceFile = filer.createSourceFile(className + "ViewBinding");
        Writer writer = sourceFile.openWriter();
        writer.write(sb.toString());
        writer.close();
    }
```
# Javapoet生成代码
1. 在lib_processor中添加依赖  
compile 'com.squareup:javapoet:1.9.0'
2. 上面的代码只能在Activity中使用，我们让它在Fragment中也可以使用。 
- Activity中生成的代码

```
public class MainActivityViewBinding {
  public MainActivityViewBinding(MainActivity target, View view) {
    target.tv=view.findViewById(2131231119);;
  }
}
```

- Fragment中生成的代码

```
public class OneFragmentViewBinding {
  public OneFragmentViewBinding(OneFragment target, View view) {
    target.tv=view.findViewById(2131231120);;
  }
}
```
3. 为了达到上面的效果我们需要改一下BindingView中的代码，添加一个view的参数

```
public class BindingView {
    public static void init(Activity activity, View view){
        try {
            Class aClass = Class.forName(activity.getClass().getCanonicalName() + "ViewBinding");
            Constructor constructor = aClass.getDeclaredConstructor(activity.getClass(), View.class);
            constructor.newInstance(activity, view);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public static void init(Fragment fragment, View view){
        try {
            Class aClass = Class.forName(fragment.getClass().getCanonicalName() + "ViewBinding");
            Constructor constructor = aClass.getDeclaredConstructor(fragment.getClass(), View.class);
            constructor.newInstance(fragment, view);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}

```
4. 使用Javapoet生成代码

```
private void generateCodeByJavapoet(List<Element> elements) throws IOException {
        String className = elements.get(0).getEnclosingElement().getSimpleName().toString();

        //声明构造方法
        MethodSpec.Builder constructMethodBuilder =
                MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC).addParameter(ClassName.bestGuess(className), "target")
                        .addParameter(VIEW, "view");
        //构造方法里面 增加语句
        for (Element e : elements) {
            constructMethodBuilder.addStatement("target." + e.getSimpleName() + "=view.findViewById(" + e.getAnnotation(BindView.class).value() + ");");
        }

        //声明类
        TypeSpec viewBindingClass =
                TypeSpec.classBuilder(className + "ViewBinding").addModifiers(Modifier.PUBLIC).addMethod(constructMethodBuilder.build()).build();
        String packageName = elementUtils.getPackageOf(elements.get(0)).getQualifiedName().toString();

        //生成Java文件
        JavaFile build = JavaFile.builder(packageName, viewBindingClass).build();
        build.writeTo(filer);
    }
    
    //在BindingProcessor中添加一个成员变量，方便生成代码，这里是参考的ButterKnife源码
    private static final ClassName VIEW = ClassName.get("android.view", "View");
```

