package software.plusminus.inject;

import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.core.ResolvableType;
import software.plusminus.util.AnnotationUtils;
import software.plusminus.util.ClassUtils;
import software.plusminus.util.FieldUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import javax.annotation.Nullable;

class InjectService {

    private ConfigurableListableBeanFactory beanFactory;
    private InjectFilter filter;

    InjectService(ConfigurableListableBeanFactory beanFactory, InjectFilter filter) {
        this.beanFactory = beanFactory;
        this.filter = filter;
    }

    void injectFields(Object bean, String beanName) {
        Object target = unwrapProxy(bean);
        FieldUtils.getFieldsStream(target.getClass())
                .filter(field -> !Modifier.isFinal(field.getModifiers()))
                .filter(field -> !field.isAnnotationPresent(Autowired.class))
                .filter(field -> !field.isAnnotationPresent(Value.class))
                .filter(field -> !field.isAnnotationPresent(NoInject.class))
                .filter(field -> !field.getDeclaringClass().isAnnotationPresent(NoInject.class))
                .filter(field -> field.getType().getPackage() != null)
                .filter(field -> !ClassUtils.isJavaClass(field.getType())
                        || Collection.class.isAssignableFrom(field.getType()))
                .filter(field -> filter.isAutoInjectable(field.getDeclaringClass()))
                .filter(field -> FieldUtils.read(target, field) == null)
                .forEach(field -> processField(bean, beanName, field));
    }

    private Object unwrapProxy(Object bean) {
        if (!AopUtils.isAopProxy(bean)) {
            return bean;
        }
        Object target = AopProxyUtils.getSingletonTarget(bean);
        return target == null ? bean : target;
    }

    private void processField(Object bean, String beanName, Field field) {
        boolean nullable = field.isAnnotationPresent(org.springframework.lang.Nullable.class)
                || field.isAnnotationPresent(Nullable.class);
        DependencyDescriptor desc = new DependencyDescriptor(field, !nullable);
        desc.setContainingClass(bean.getClass());
        Object injectCandidate;
        try { 
            injectCandidate = beanFactory.resolveDependency(desc, beanName, null, beanFactory.getTypeConverter());
        } catch (BeansException e) {
            if (isTestClass(field)) {
                return;
            }
            throw e;
        }
        if (injectCandidate == null) {
            if (nullable) {
                return;
            }
            throw new NoUniqueBeanDefinitionException(ResolvableType.forField(field));
        }
        Object target = AopUtils.isAopProxy(bean) ? AopProxyUtils.getSingletonTarget(bean) : bean;
        if (target == null) {
            target = bean;
        }
        FieldUtils.write(target, injectCandidate, field);
    }
    
    private boolean isTestClass(Field field) {
        Annotation annotation = AnnotationUtils.findAnnotation("org.junit.runner.RunWith", field.getDeclaringClass());
        return annotation != null;
    }
    
}
