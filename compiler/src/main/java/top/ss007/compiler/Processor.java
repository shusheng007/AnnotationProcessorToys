package top.ss007.compiler;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;

import top.ss007.annotation.BindView;
import top.ss007.annotation.Keep;
import top.ss007.annotation.OnClick;

public class Processor extends AbstractProcessor {
    private Filer filer;
    private Messager messager;
    private Elements elementUtils;

    //每个存在注解的类整理出来，key:package_classname value:被注解的类型元素
    private Map<String, List<Element>> annotationClassMap = new HashMap<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        filer = processingEnv.getFiler();
        messager = processingEnv.getMessager();
        elementUtils = processingEnv.getElementUtils();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (annotations == null || annotations.isEmpty()) {
            return false;
        }

        if (!roundEnv.processingOver()) {
            annotationClassMap.clear();
            buildAnnotatedElement(roundEnv, BindView.class);
            buildAnnotatedElement(roundEnv, OnClick.class);

            for (Map.Entry<String, List<Element>> entry : annotationClassMap.entrySet()) {
                String packageName = entry.getKey().split("_")[0];
                String typeName = entry.getKey().split("_")[1];
                ClassName className = ClassName.get(packageName, typeName);
                ClassName generatedClassName = ClassName
                        .get(packageName, NameStore.getGeneratedClassName(typeName));

                /*
                创建要生成的类，如下所示
                @Keep
                public class MainActivity$Binding {}*/
                TypeSpec.Builder classBuilder = TypeSpec.classBuilder(generatedClassName)
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Keep.class);

                /*添加构造函数
                *   public MainActivity$Binding(MainActivity activity) {
                    bindViews(activity);
                    bindOnClicks(activity);
                  }
                */
                classBuilder.addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(className, NameStore.Variable.ANDROID_ACTIVITY)
                        .addStatement("$N($N)",
                                NameStore.Method.BIND_VIEWS,
                                NameStore.Variable.ANDROID_ACTIVITY)
                        .addStatement("$N($N)",
                                NameStore.Method.BIND_ON_CLICKS,
                                NameStore.Variable.ANDROID_ACTIVITY)
                        .build());

                /*创建方法bindViews(MainActivity activity)
                * private void bindViews(MainActivity activity) {}
                */
                MethodSpec.Builder bindViewsMethodBuilder = MethodSpec
                        .methodBuilder(NameStore.Method.BIND_VIEWS)
                        .addModifiers(Modifier.PRIVATE)
                        .returns(void.class)
                        .addParameter(className, NameStore.Variable.ANDROID_ACTIVITY);

                /*增加方法体
                * activity.tvHello = (TextView)activity.findViewById(2131165326);
                * */
                for (VariableElement variableElement : ElementFilter.fieldsIn(entry.getValue())) {
                    BindView bindView = variableElement.getAnnotation(BindView.class);
                    if (bindView != null) {
                        bindViewsMethodBuilder.addStatement("$N.$N = ($T)$N.findViewById($L)",
                                NameStore.Variable.ANDROID_ACTIVITY,
                                variableElement.getSimpleName(),
                                variableElement,
                                NameStore.Variable.ANDROID_ACTIVITY,
                                bindView.value());
                    }
                }
                //将构建出来的方法添加到类里面
                classBuilder.addMethod(bindViewsMethodBuilder.build());

                /*以下构建如下代码
                *   private void bindOnClicks(final MainActivity activity) {
                    activity.findViewById(2131165218).setOnClickListener(new View.OnClickListener() {
                      public void onClick(View view) {
                        activity.onHelloBtnClick(view);
                      }
                    });
                  }
                 */
                ClassName androidOnClickListenerClassName = ClassName.get(
                        NameStore.Package.ANDROID_VIEW,
                        NameStore.Class.ANDROID_VIEW,
                        NameStore.Class.ANDROID_VIEW_ON_CLICK_LISTENER);

                ClassName androidViewClassName = ClassName.get(
                        NameStore.Package.ANDROID_VIEW,
                        NameStore.Class.ANDROID_VIEW);

                MethodSpec.Builder bindOnClicksMethodBuilder = MethodSpec
                        .methodBuilder(NameStore.Method.BIND_ON_CLICKS)
                        .addModifiers(Modifier.PRIVATE)
                        .returns(void.class)
                        .addParameter(className, NameStore.Variable.ANDROID_ACTIVITY, Modifier.FINAL);

                for (ExecutableElement executableElement : ElementFilter.methodsIn(entry.getValue())) {
                    OnClick onClick = executableElement.getAnnotation(OnClick.class);
                    if (onClick != null) {
                        TypeSpec onClickListenerClass = TypeSpec.anonymousClassBuilder("")
                                .addSuperinterface(androidOnClickListenerClassName)
                                .addMethod(MethodSpec.methodBuilder(NameStore.Method.ANDROID_VIEW_ON_CLICK)
                                        .addModifiers(Modifier.PUBLIC)
                                        .addParameter(androidViewClassName, NameStore.Variable.ANDROID_VIEW)
                                        .addStatement("$N.$N($N)",
                                                NameStore.Variable.ANDROID_ACTIVITY,
                                                executableElement.getSimpleName(),
                                                NameStore.Variable.ANDROID_VIEW)
                                        .returns(void.class)
                                        .build())
                                .build();
                        bindOnClicksMethodBuilder.addStatement("$N.findViewById($L).setOnClickListener($L)",
                                NameStore.Variable.ANDROID_ACTIVITY,
                                onClick.value(),
                                onClickListenerClass);
                    }
                }
                classBuilder.addMethod(bindOnClicksMethodBuilder.build());

                //将类写入文件中
                try {
                    JavaFile.builder(packageName,
                            classBuilder.build())
                            .build()
                            .writeTo(filer);
                } catch (IOException e) {
                    messager.printMessage(Diagnostic.Kind.ERROR, e.toString());
                }
            }
        }
        return true;
    }


    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return new TreeSet<>(Arrays.asList(
                BindView.class.getCanonicalName(),
                OnClick.class.getCanonicalName(),
                Keep.class.getCanonicalName()));
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    private void buildAnnotatedElement(RoundEnvironment roundEnv, Class<? extends Annotation> clazz) {
        for (Element element : roundEnv.getElementsAnnotatedWith(clazz)) {
            String className = getFullClassName(element);
            List<Element> cacheElements = annotationClassMap.get(className);
            if (cacheElements == null) {
                cacheElements = new ArrayList<>();
                annotationClassMap.put(className, cacheElements);
            }
            cacheElements.add(element);
        }
    }

    private String getFullClassName(Element element) {
        TypeElement typeElement = (TypeElement) element.getEnclosingElement();
        String packageName = elementUtils.getPackageOf(typeElement).getQualifiedName().toString();
        return packageName + "_" + typeElement.getSimpleName().toString();
    }
}
