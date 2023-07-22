package de.darxun.companion.container.model;

import de.darxun.companion.container.util.BeanDefinitionHelper;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BeanDefinition extends BeanDependency {

    private final List<BeanDependency> dependencies;

    private Constructor constructor;

    public BeanDefinition(final Class<?> clazz, final String beanId) {
        super(clazz, beanId);
        this.dependencies = new ArrayList<>();
    }

    public BeanDefinition(Class<?> clazz) {
        this(clazz, BeanDefinitionHelper.getBeanId(clazz));
    }

    public void addDependency(BeanDependency dependecy) {
        this.dependencies.add(dependecy);
    }

    public List<BeanDependency> getDependencies() {
        return Collections.unmodifiableList(dependencies);
    }

    public Constructor getConstructor() {
        return constructor;
    }

    public void setConstructor(Constructor constructor) {
        this.constructor = constructor;
    }

    @Override
    public String toString() {
        return "BeanDefinition{" +
                "clazz=" + getClazz() +
                ", id='" + getId() + '\'' +
                '}';
    }
}
