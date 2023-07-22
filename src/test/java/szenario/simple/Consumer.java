package szenario.simple;

import de.darxun.companion.api.Bean;

import javax.inject.Inject;
import javax.inject.Named;

@Bean("myConsumer")
public class Consumer {

    private Provider provider;

    private AnotherProvider anotherProvider;

    @Inject
    public Consumer(@Named("someProvider") Provider provider, AnotherProvider anotherProvider) {
        this.provider = provider;
        this.anotherProvider = anotherProvider;
    }

    public String doConsume() {
        return "Providerdata by consumer: " + provider.getData() + " & more data: " + anotherProvider.getData();
    }
}
