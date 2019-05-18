package top.ss007.compiler;

import java.util.HashSet;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

public class ProcessingUtils {

    private ProcessingUtils() {
        // not to be instantiated in public
    }

    /**
     * 获得使用了我们支持的注解的类集合，例如MainActivity 里面的一个TextView 使用BindView注解了，那么MainActivity就会在这个集合里面
     * @param elements 此轮编译中的所有程序元素
     * @param supportedAnnotations 支持的注解 例如BindView, OnClick
     * @return 存在类里面元素使用了BindView 或者OnClick 标注的类的集合
     */
    public static Set<TypeElement> getTypeElementsToProcess(Set<? extends Element> elements,
                                                            Set<? extends Element> supportedAnnotations) {
        Set<TypeElement> typeElements = new HashSet<>();
        for (Element element : elements) {
            if (element instanceof TypeElement) {//是类或者接口，此处要找MainActivity
                boolean found = false;
                for (Element subElement : element.getEnclosedElements()) {//element.getEnclosedElements()此类型中包含的所有元素，包括属性，构造函数，方法。。。
                    for (AnnotationMirror mirror : subElement.getAnnotationMirrors()) {//subElement.getAnnotationMirrors() 某一元素上的所有注解，例如BindView
                        for (Element annotation : supportedAnnotations) {//看某一个元素上的注解有没有是我们支持的，也就是说，有没有使用BindView或者Onclick注释
                            if (mirror.getAnnotationType().asElement().equals(annotation)) {
                                typeElements.add((TypeElement) element);
                                found = true;
                                break;
                            }
                        }
                        if (found) break;
                    }
                    if (found) break;
                }
            }
        }
        return typeElements;
    }
}
