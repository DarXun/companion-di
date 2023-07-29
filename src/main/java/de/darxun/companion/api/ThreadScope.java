package de.darxun.companion.api;

import javax.inject.Scope;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Sets the scope for a bean as thread-scope.
 * The lifecycle of a thread-scope bean is bound to the current thread.
 */
@Scope
@Target({ TYPE, METHOD })
@Retention(RUNTIME)
public @interface ThreadScope {
}
