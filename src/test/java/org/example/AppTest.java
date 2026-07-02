package org.example;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AppTest {
    @Test
    void returnsGreeting() {
        assertThat(App.greeting()).isEqualTo("Hello, Java 25");
    }
}
