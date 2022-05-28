package software.plusminus.inject;

import org.springframework.core.annotation.MergedAnnotations;

import java.lang.reflect.Proxy;

class InjectFilter {
    
    boolean shouldBeProcessed(Object bean) {
        return !isConfigurationProperties(bean) && !isSpringClass(bean);
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
