package de.darxun.companion;

/**
 * Exception to address problems computing a BeanDefinition.
 */
public class BeanComputationException extends RuntimeException {

    public BeanComputationException() {
    }

    public BeanComputationException(String message) {
        super(message);
    }

    public BeanComputationException(String message, Throwable cause) {
        super(message, cause);
    }

    public BeanComputationException(Throwable cause) {
        super(cause);
    }

    public BeanComputationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
