package software.plusminus.inject;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.MergedAnnotations;
import software.plusminus.util.ClassUtils;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

class InjectFilter {
    
    private List<String> include = new ArrayList<>();
    private List<String> exclude = new ArrayList<>();

    InjectFilter(ConfigurableListableBeanFactory beanFactory) {
        populateList(beanFactory, AutoInject.class, AutoInject::value, include);
        populateList(beanFactory, NoInject.class, NoInject::value, exclude);
    }

    boolean shouldBeProcessed(Object bean) {
        return !include.isEmpty()
                && isAutoInjectable(bean.getClass())
                && !isConfigurationProperties(bean)
                && !ClassUtils.isJavaClass(bean.getClass());
    }
    
    boolean isAutoInjectable(Class<?> c) {
        String beanClassName = c.getName();
        return exclude.stream().noneMatch(beanClassName::startsWith)
                && include.stream().anyMatch(beanClassName::startsWith);
    }

    private <A extends Annotation> void populateList(ConfigurableListableBeanFactory beanFactory,
                                                     Class<A> annotationType,
                                                     Function<A, String[]> packagesFunction,
                                                     List<String> list) {
        Map<String, ?> beansWithAnnotation = beanFactory.getBeansWithAnnotation(annotationType);
        beansWithAnnotation.values().forEach(bean -> {
            A annotation = AnnotationUtils.findAnnotation(bean.getClass(), annotationType);
            String[] packages = packagesFunction.apply(annotation);
            if (packages.length == 0) {
                list.add(bean.getClass().getPackage().getName() + '.');
            } else {
                Stream.of(packages)
                        .map(p -> p.endsWith(".") ? p : p + '.')
                        .forEach(list::add);
            }
        });
    }

    private boolean isConfigurationProperties(Object bean) {
        return MergedAnnotations.from(bean.getClass(), MergedAnnotations.SearchStrategy.TYPE_HIERARCHY)
                .isPresent("org.springframework.boot.context.properties.ConfigurationProperties");
    }
}
