package szenario.simple;

import de.darxun.companion.api.Bean;

@Bean("someProvider")
public class Provider {

    public String getData() {
        return "some data";
    }

    public Provider() {
    }
}
