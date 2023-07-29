package de.darxun.companion.container;

import de.darxun.companion.api.Bean;

import javax.inject.Inject;

@Bean("someProvider")
public class Provider {

    public String getData() {
        return "some data";
    }

    public Provider() {
    }
}
