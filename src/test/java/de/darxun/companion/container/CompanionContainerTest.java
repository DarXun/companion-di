package de.darxun.companion.container;

import de.darxun.companion.BeanNotFoundException;
import de.darxun.companion.container.util.BeanDefinitionHelper;
import org.junit.jupiter.api.Test;
import szenario.simple.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class CompanionContainerTest {

    @Test
    void print() {
        CompanionContainer container = CompanionContainer.setup();

        Consumer consumer = container.getBean("myConsumer", Consumer.class);
        assertEquals(consumer, container.getBean(Consumer.class));
        assertNotNull(container.getBean("someProvider"));
        assertThrows(BeanNotFoundException.class, () -> container.getBean(BeanDefinitionHelper.getBeanId(Consumer.class), Consumer.class));

        System.out.println(consumer.doConsume());
        System.out.println(consumer.add(2, 3));
        System.out.println(consumer.multiply(2, 3));
        System.out.println(consumer.divide(10, 2));

        for (int i = 0; i <= 10; i++) {
            new Thread(() -> {
                Thread currentThread = Thread.currentThread();
                Thread threadFromThreadScopeBean = consumer.getThreadFromThreadScopeBean();
                assertEquals(currentThread, threadFromThreadScopeBean);
                System.out.println(String.format("Current thread: %s, Thread from ThreadScopeBean: %s", currentThread, threadFromThreadScopeBean));
            }).start();
        }
    }
}