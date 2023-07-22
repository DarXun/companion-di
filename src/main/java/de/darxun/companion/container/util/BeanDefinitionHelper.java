package de.darxun.companion.container.util;

public class BeanDefinitionHelper {

    /**
     * Returns the beanId for the specified class
     * @param clazz the class
     * @return the beanId
     * @param <T> the type of the bean
     */
    public static <T extends Object> String getBeanId(Class<T> clazz) {
        return clazz.getName();
    }
}
