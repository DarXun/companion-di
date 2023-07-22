package de.darxun.companion.container.util;

import de.darxun.companion.api.Bean;

import javax.inject.Inject;
import javax.inject.Named;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ReflectionHelper {

    /**
     * Returns true if the specified class-level-annotation is present on the given class
     * @param clazz the class to analyze
     * @param annotationClazz the class-level-annotation to look for
     * @return
     */
    public static boolean hasClassAnnotation(Class<?> clazz, Class<? extends Annotation> annotationClazz) {
        return clazz.getAnnotation(annotationClazz) != null;
    }

    /**
     * Returns the methods the specified method-level-annotation is present on
     * @param clazz the class to analyze
     * @param annotationClazz the method-level-annotation to look for
     * @return set of methods the annotation is present on
     */
    public static Set<Method> getMethodWithAnnotation(Class<?> clazz, Class<? extends Annotation> annotationClazz) {
        // for performance measures we do not if this is a method-level-annotation
        return Arrays.stream(clazz.getMethods()).filter(method -> method.isAnnotationPresent(annotationClazz)).collect(Collectors.toSet());
    }

    /**
     * Returns the single constructor the @Inject-Annotation is present on.
     * Throws an exception if no or more than one constructors are found
     * @param clazz the class to analyse
     * @return the injectable constructor
     */
    public static Constructor getInjectableConstructor(Class<?> clazz) {
        Constructor<?>[] constructors = clazz.getConstructors();

        if (constructors.length == 0) {
            throw new IllegalStateException(String.format("No constructors found for Class %s", clazz));
        }

        if (constructors.length == 1) {
            return constructors[0];
        }

        List<Constructor<?>> injectableConstructors = Arrays.stream(constructors).filter(constructor -> constructor.isAnnotationPresent(Inject.class)).collect(Collectors.toList());
        if (injectableConstructors.size() == 1) {
            return injectableConstructors.get(0);
        }

        throw new IllegalStateException("No single injectable constructor found");
    }

    /**
     * Returns the bean id if present
     * @param clazz the clazz to analyze
     * @return the bean id or null
     */
    public static String getBeanId(Class<?> clazz) {
        Bean annotation = clazz.getAnnotation(Bean.class);

        if (annotation == null) {
            throw new IllegalStateException("@Bean-Annotation not present.");
        }

        // empty string is treated as if no bean id was specified
        return annotation.value().equals("") ? null : annotation.value();
    }

    /**
     * Returns a String-Array containing the specified (via @Named) or derived (via type) bean ids.
     * @param injectableConstructor Constructor to analyse
     * @return bean ids
     */
    public static String[] getBeanIdsForDependencies(Constructor injectableConstructor) {
        Parameter[] parameters = injectableConstructor.getParameters();
        String[] beanIds = new String[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];

            Named namedAnnotation = parameter.getAnnotation(Named.class);

            String parameterBeanId = null;
            if (namedAnnotation != null) {
                String namedBeanId = namedAnnotation.value();

                if (namedBeanId.trim().length() == 0) {
                    throw new IllegalArgumentException(String.format("The id (%s) is not a valid bean id", namedBeanId));
                }

                parameterBeanId = namedBeanId;
            }

            beanIds[i] = parameterBeanId;
        }

        return beanIds;
    }
}
