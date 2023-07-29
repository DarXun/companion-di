package de.darxun.companion.container.model.beansupplier;

import de.darxun.companion.container.model.BeanDefinition;

import java.lang.reflect.Proxy;
import java.util.Set;
import java.util.function.Supplier;

public class ThreadScopeBeanSupplier implements BeanSupplier {

    private final ThreadLocal<Object> threadLocalInstance;

    private final BeanDefinition beanDefinition;

    private final Supplier<Object> instantiator;

    public ThreadScopeBeanSupplier(BeanDefinition beanDefinition, Supplier<Object> instantiator) {
        this.threadLocalInstance = new ThreadLocal<>();
        this.beanDefinition = beanDefinition;
        this.instantiator = instantiator;
    }

    @Override
    public Object get() {
        return createProxyInstance();
    }

    private Object getThreadBoundInstance() {
        Object instance = threadLocalInstance.get();
        if (instance == null) {
            instance = instantiator.get();
            threadLocalInstance.set(instance);
        }

        return instance;
    }

    private Object createProxyInstance() {
        Set<Class<?>> interfacesSet = beanDefinition.getInterfaces();
        Class<?>[] interfaces = interfacesSet.toArray(new Class<?>[interfacesSet.size()]);

        return Proxy.newProxyInstance(beanDefinition.getClass().getClassLoader(), interfaces, (proxy, method, args) -> {
            Object instance = getThreadBoundInstance();

            return method.invoke(instance, args);
        });
    }
}
