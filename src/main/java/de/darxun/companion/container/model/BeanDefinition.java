package de.darxun.companion.container.model;

import de.darxun.companion.container.util.BeanDefinitionHelper;

import java.lang.reflect.Constructor;
import java.util.*;

public class BeanDefinition extends BeanDependency {

    private final List<BeanDependency> dependencies;

    private Constructor constructor;

    private Set<Class<?>> interfaces;

    private Set<Class<?>> superclasses;

    public BeanDefinition(final Class<?> clazz, final String beanId) {
        super(clazz, beanId);
        this.dependencies = new ArrayList<>();
        this.interfaces = new HashSet<>();
        this.superclasses = new HashSet<>();
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

    public void addInterface(Class<?> interfaceClazz) {
        this.interfaces.add(interfaceClazz);
    }

    public void addInterfaces(Set<Class<?>> interfaces) {
        this.interfaces.addAll(interfaces);
    }

    public void addSuperclass(Class<?> superClazz) {
        this.superclasses.add(superClazz);
    }

    public Set<Class<?>> getInterfaces() {
        return Collections.unmodifiableSet(interfaces);
    }

    public Set<Class<?>> getSuperclasses() {
        return Collections.unmodifiableSet(superclasses);
    }

    public Set<Class<?>> getInterfacesAndSuperclasses() {
        Set<Class<?>> classes = Collections.unmodifiableSet(interfaces);
        classes.addAll(superclasses);

        return classes;
    }

    @Override
    public String toString() {
        return "BeanDefinition{" +
                "clazz=" + getClazz() +
                ", id='" + getId() + '\'' +
                '}';
    }
}
