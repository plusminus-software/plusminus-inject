package software.plusminus.inject;

import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.core.ResolvableType;
import software.plusminus.util.ClassUtils;
import software.plusminus.util.FieldUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import javax.annotation.Nullable;

class InjectService {

    private ConfigurableListableBeanFactory beanFactory;
    private InjectFilter filter;

    InjectService(ConfigurableListableBeanFactory beanFactory, InjectFilter filter) {
        this.beanFactory = beanFactory;
        this.filter = filter;
    }

    void injectFields(Object bean, String beanName) {
        FieldUtils.getFieldsStream(bean.getClass())
                .filter(field -> !Modifier.isFinal(field.getModifiers()))
                .filter(field -> !field.isAnnotationPresent(Autowired.class))
                .filter(field -> !field.isAnnotationPresent(Value.class))
                .filter(field -> !field.isAnnotationPresent(NoInject.class))
                .filter(field -> !field.getDeclaringClass().isAnnotationPresent(NoInject.class))
                .filter(field -> field.getType().getPackage() != null)
                .filter(field -> !ClassUtils.isJavaClass(field.getType()))
                .filter(field -> filter.isAutoInjectable(field.getDeclaringClass()))
                .filter(field -> FieldUtils.read(bean, field) == null)
                .forEach(field -> processField(bean, beanName, field));
    }

    private void processField(Object bean, String beanName, Field field) {
        boolean nullable = field.isAnnotationPresent(org.springframework.lang.Nullable.class)
                || field.isAnnotationPresent(Nullable.class);
        DependencyDescriptor desc = new DependencyDescriptor(field, !nullable);
        desc.setContainingClass(bean.getClass());
        Object injectCandidate = beanFactory.resolveDependency(desc, beanName, null, beanFactory.getTypeConverter());
        if (injectCandidate == null) {
            throw new NoUniqueBeanDefinitionException(ResolvableType.forField(field));
        }
        if (AopUtils.isCglibProxy(bean)) {
            Object unproxied = AopProxyUtils.getSingletonTarget(bean);
            FieldUtils.write(unproxied, injectCandidate, field);
        }
        FieldUtils.write(bean, injectCandidate, field);
    }
    
}
