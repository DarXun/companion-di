package de.darxun.companion.container;

public class SuperWorker extends AbstractSuperWorker {

    public int multiply(int a, int b) {
        return a * b;
    }

    @Override
    public double divide(int a, int b) {
        return (double) a / (double) b;
    }
}
