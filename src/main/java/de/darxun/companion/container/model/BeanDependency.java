package de.darxun.companion.container.model;

import java.util.Objects;

public class BeanDependency {
    private final Class<?> clazz;

    private final String id;

    public BeanDependency(Class<?> clazz, String id) {
        this.clazz = clazz;
        this.id = id;
    }

    public BeanDependency(Class<?> clazz) {
        this(clazz, null);
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return "BeanDependency{" +
                "clazz=" + clazz +
                ", id='" + id + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BeanDependency that = (BeanDependency) o;
        return Objects.equals(clazz, that.clazz) && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clazz, id);
    }
}
