package de.darxun.companion.container.model;

public class SingletonBeanSupplier implements BeanSupplier {

    private final Object instance;

    public SingletonBeanSupplier(Object instance) {
        this.instance = instance;
    }

    @Override
    public Object get() {
        return instance;
    }
}
