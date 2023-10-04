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
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.stereotype.Component;
import software.plusminus.util.AnnotationUtils;
import software.plusminus.util.MethodUtils;

import javax.annotation.PostConstruct;

@Component
public class InjectBeanPostProcessor implements BeanPostProcessor {

    private InjectFilter filter;
    private InjectService service;

    public InjectBeanPostProcessor(ConfigurableListableBeanFactory beanFactory) {
        this.filter = new InjectFilter(beanFactory);
        this.service = new InjectService(beanFactory, this.filter);
    }

    @Override
    @SuppressWarnings("squid:RedundantThrowsDeclarationCheck")
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (filter.shouldBeProcessed(bean)) {
            boolean mustInitializeNow = MethodUtils.getMethodsStream(bean.getClass())
                    .anyMatch(method -> AnnotationUtils.findAnnotation(PostConstruct.class, method) != null);
            if (mustInitializeNow) {
                service.injectFields(bean, beanName);
            }
        }
        return bean;
    }

    @Override
    @SuppressWarnings("squid:RedundantThrowsDeclarationCheck")
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (filter.shouldBeProcessed(bean)) {
            service.injectFields(bean, beanName);
        }
        return bean;
    }
}
