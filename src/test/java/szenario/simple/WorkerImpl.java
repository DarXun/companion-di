package szenario.simple;

import de.darxun.companion.api.Bean;

@Bean
public class WorkerImpl extends SuperWorker implements Worker {
    @Override
    public int add(int a, int b) {
        return a + b;
    }
}
