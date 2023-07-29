package de.darxun.companion.container;

import de.darxun.companion.api.Bean;

import javax.inject.Inject;

@Bean
public class AnotherProvider {

    private Provider provider;

    @Inject
    public AnotherProvider(Provider provider) {
        this.provider = provider;
    }

    public String getData() {
        return "other data and data from provider: " + provider.getData();
    }

}
