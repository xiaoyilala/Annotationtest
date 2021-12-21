package com.ice.annotation.lib_processor;

import static javax.lang.model.element.Modifier.PUBLIC;

import com.ice.annotation.lib_interface.BindView;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

//package com.ice.annotationtest;
//
///**
// * 要获取哪些类使用了我们的BindView注解；
// * 获取这些类中使用了BindView注解的field以及他们对应的值；
// * 拿到这些类的类名称以便我们生成诸如MainActivityViewBinding这样的类名；
// * 拿到这些类的包名，因为我们生成的类要和注解所属的类属于同一个package 才不会出现field 访问权限的问题；
// * */
//public class MainActivityViewBinding {
//
//    public MainActivityViewBinding(MainActivity activity) {
//        activity.tv = activity.findViewById(R.id.tv);
//    }
//}

/**
 * 注解处理类
 * */
public class BindingProcessor extends AbstractProcessor {

    private static final ClassName VIEW = ClassName.get("android.view", "View");
    private static final ClassName UNBINDER = ClassName.get("com.ice.annotation.lib_interface", "Unbinder");

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
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // key 就是使用注解的class的类名 element就是使用注解本身的元素 一个class 可以有多个使用注解的field
        Map<String, List<Element>> fieldMap = new HashMap<>();
        // 这里 获取到 所有使用了 BindView 注解的 element
        for(Element element: roundEnv.getElementsAnnotatedWith(BindView.class)){

            TypeMirror typeMirror = element.asType();
            TypeName typeName = ClassName.get(typeMirror);

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
//                generateCodeByStringBuffer(entry.getValue());
//                generateCodeByJavapoetActivity(entry.getKey(), entry.getValue());
                generateCodeByJavapoet(entry.getValue());
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

    private void generateCodeByJavapoetActivity(String className, List<Element> elements) throws IOException {

//        MethodSpec mainSpec = MethodSpec.methodBuilder("main")         // 方法名
//                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)            // 修饰符
//                .addParameter(String[].class, "args")                      // 参数
//                .returns(TypeName.VOID)                                    // 返回值
//                .addStatement("$T.out.println($S)", System.class, "Hello,World!!")  // 具体代码
//                .build();

//        // 添加代码的方法
//        mainBuilder.addCode("System.out.println(\"Hello,World!\")");
//        mainBuilder.addStatement("$T.out.println($S)",System.class,"Hello, World!");
//        mainBuilder.addStatement("activity.$L= ($T) activity.findViewById($L)",
//                element, ClassName.get(member.asType()), injectViewAnno.value() );
//        // 这里的element表示一个成员变量， injectViewAnno是一个Annotation， value是注解的参数值
//        添加代码：addStatement末尾会自动添加换行符，addCode末位不会自动添加换行符
//                -$T：表示需要import的类
//                -$S：表示字符串，会自动添加双引号
//                -$L：表示变量，不带双引号

        //声明构造方法
        MethodSpec.Builder constructMethodBuilder =
                MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC).addParameter(ClassName.bestGuess(className), "activity");
        //构造方法里面 增加语句
        for (Element e : elements) {
            constructMethodBuilder.addStatement("activity." + e.getSimpleName() + "=activity.findViewById(" + e.getAnnotation(BindView.class).value() + ");");
        }

//        // 外部类
//        TypeSpec typeSpec = TypeSpec.classBuilder("HelloWorld")         // 类名
//                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)              // 修饰符
//                .addMethod(mainSpec)                                        // 方法
//                .build();
//        // 内部类
//        TypeSpec typeSpec = TypeSpec.anonymousClassBuilder("innerClass")    // 内部类的类名
//                .addSuperinterface(OutClass.class)                              // 外部类的Class
//                .build();

        //声明类
        TypeSpec viewBindingClass =
                TypeSpec.classBuilder(className + "ViewBinding").addModifiers(Modifier.PUBLIC).addMethod(constructMethodBuilder.build()).build();
        String packageName = elementUtils.getPackageOf(elements.get(0)).getQualifiedName().toString();

        //生成Java文件
        JavaFile build = JavaFile.builder(packageName, viewBindingClass).build();
        build.writeTo(filer);
    }

    private void generateCodeByJavapoet(List<Element> elements) throws IOException {
        String className = elements.get(0).getEnclosingElement().getSimpleName().toString();

        //声明构造方法
        MethodSpec.Builder constructMethodBuilder =
                MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC).addParameter(ClassName.bestGuess(className), "target")
                        .addParameter(VIEW, "view")
                        .addStatement("this.target = target");

        //解绑的方法
        MethodSpec.Builder unbindBuild = MethodSpec.methodBuilder("unbind")
                .addAnnotation(Override.class)
                .addModifiers(PUBLIC)
                .addStatement("$T target = this.target", ClassName.bestGuess(className))
                .addStatement("$N = null", "this.target");

        for (Element e : elements) {
            //构造方法里面 增加语句
            constructMethodBuilder.addStatement("target." + e.getSimpleName() + "=view.findViewById(" + e.getAnnotation(BindView.class).value() + ");");
            //解绑方法里面添加语句
            unbindBuild.addStatement("target.$L = null", e.getSimpleName());
        }

        //声明类
        TypeSpec viewBindingClass =
                TypeSpec.classBuilder(className + "ViewBinding")
                        .addModifiers(Modifier.PUBLIC)
                        .addSuperinterface(UNBINDER)
                        .addField(ClassName.bestGuess(className), "target")
                        .addMethod(constructMethodBuilder.build())
                        .addMethod(unbindBuild.build())
                        .build();
        String packageName = elementUtils.getPackageOf(elements.get(0)).getQualifiedName().toString();

        //生成Java文件
        JavaFile build = JavaFile.builder(packageName, viewBindingClass).build();
        build.writeTo(filer);
    }

    //要支持哪些注解
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(BindView.class.getCanonicalName());
    }
}