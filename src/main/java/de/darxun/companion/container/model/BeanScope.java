package de.darxun.companion.container.model;

/**
 * The BeanScope determins a beans lifecycle
 */
public enum BeanScope {
    /** The default-scope - there's exactly one instance in the container for a singleton-bean */
    Singleton,
    /** With the thread-scope there's an instance for every instance for a thread-scope-bean */
    Thread;
}
