package de.darxun.companion.api;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks a Configuration-Class containing @Bean-Definitions
 */
@Bean
@Target({ TYPE })
@Retention(RUNTIME)
public @interface Configuration {
}
