/*
 * Copyright 2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package software.plusminus.inject;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.stereotype.Component;
import software.plusminus.util.ClassUtils;
import software.plusminus.util.FieldUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import javax.annotation.Nullable;

@Component
public class InjectBeanPostProcessor implements BeanPostProcessor {

    private ConfigurableListableBeanFactory beanFactory;

    public InjectBeanPostProcessor(ConfigurableListableBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @Override
    @SuppressWarnings("squid:RedundantThrowsDeclarationCheck")
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (AnnotationUtils.findAnnotation(bean.getClass(), Component.class) == null
                || isConfigurationProperties(bean)
                || isSpringClass(bean)) {
            return bean;
        }
        FieldUtils.getFieldsStream(bean.getClass())
                .filter(field -> !Modifier.isFinal(field.getModifiers()))
                .filter(field -> !field.isAnnotationPresent(Autowired.class))
                .filter(field -> !field.isAnnotationPresent(Value.class))
                .filter(field -> field.getType().getPackage() != null)
                .filter(field -> !ClassUtils.isJvmClass(field.getType()))
                .filter(field -> FieldUtils.read(bean, field) == null)
                .forEach(field -> processField(bean, beanName, field));
        return bean;
    }
    
    private void processField(Object bean, String beanName, Field field) {
        boolean nullable = field.isAnnotationPresent(org.springframework.lang.Nullable.class)
                || field.isAnnotationPresent(Nullable.class);
        DependencyDescriptor desc = new DependencyDescriptor(field, !nullable);
        desc.setContainingClass(bean.getClass());
        Object injectCandidate = beanFactory.resolveDependency(desc, beanName, null, beanFactory.getTypeConverter());
        if (injectCandidate != null) {
            FieldUtils.write(bean, injectCandidate, field);
        } else {
            throw new NoUniqueBeanDefinitionException(ResolvableType.forField(field));
        }
    }
    
    private boolean isConfigurationProperties(Object bean) {
        return MergedAnnotations.from(bean.getClass(), MergedAnnotations.SearchStrategy.TYPE_HIERARCHY)
                .isPresent("org.springframework.boot.context.properties.ConfigurationProperties");
    }
    
    private boolean isSpringClass(Object bean) {
        String packageName;
        if (Proxy.isProxyClass(bean.getClass())) {
            Class<?>[] interfaces = bean.getClass().getInterfaces();
            if (interfaces.length == 0) {
                return false;
            }
            packageName = interfaces[0].getPackage().getName();
        } else {
            packageName = bean.getClass().getPackage().getName();
        }
        return packageName.startsWith("org.springframework");
    }
}
