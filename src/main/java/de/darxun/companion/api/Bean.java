package de.darxun.companion.api;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks a bean that should be controlled by the container
 */
@Target({ TYPE, METHOD })
@Retention(RUNTIME)
public @interface Bean {

    /** the bean id */
    String value() default "";

}
