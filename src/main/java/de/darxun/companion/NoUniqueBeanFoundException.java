package de.darxun.companion;

public class NoUniqueBeanFoundException extends BeanNotFoundException{

    public NoUniqueBeanFoundException() {
    }

    public NoUniqueBeanFoundException(String message) {
        super(message);
    }

    public NoUniqueBeanFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoUniqueBeanFoundException(Throwable cause) {
        super(cause);
    }

    public NoUniqueBeanFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
