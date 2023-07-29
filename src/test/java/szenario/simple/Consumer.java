package szenario.simple;

import de.darxun.companion.api.Bean;

import javax.inject.Inject;
import javax.inject.Named;

@Bean("myConsumer")
public class Consumer {

    private Provider provider;

    private AnotherProvider anotherProvider;

    private Worker worker;

    private SuperWorker superWorker;

    private AbstractSuperWorker abstractSuperWorker;

    private ThreadScopeBean threadScopeBean;

    @Inject
    public Consumer(@Named("someProvider") Provider provider, AnotherProvider anotherProvider, Worker worker, SuperWorker superWorker, AbstractSuperWorker abstractSuperWorker, ThreadScopeBean threadScopeBean) {
        this.provider = provider;
        this.anotherProvider = anotherProvider;
        this.worker = worker;
        this.superWorker = superWorker;
        this.abstractSuperWorker = abstractSuperWorker;
        this.threadScopeBean = threadScopeBean;
    }

    public String doConsume() {
        return "Providerdata by consumer: " + provider.getData() + " & more data: " + anotherProvider.getData();
    }

    public String add(int x, int y) {
        return String.format("%d + %d = %d", x, y, worker.add(x, y));
    }

    public String multiply(int x, int y) {
        return String.format("%d * %d = %d", x, y, superWorker.multiply(x, y));
    }

    public String divide(int x, int y) {
        return String.format("%d / %d = %f", x, y, abstractSuperWorker.divide(x, y));
    }

    public Thread getThreadFromThreadScopeBean() {
        return threadScopeBean.getThread();
    }
}
