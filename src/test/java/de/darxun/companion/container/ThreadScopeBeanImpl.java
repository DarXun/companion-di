package de.darxun.companion.container;

import de.darxun.companion.api.Bean;
import de.darxun.companion.api.ThreadScope;

@Bean
@ThreadScope
public class ThreadScopeBeanImpl implements ThreadScopeBean {

    @Override
    public Thread getThread() {
        return Thread.currentThread();
    }
}
