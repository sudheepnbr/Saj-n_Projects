package com.oih.idp.web;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class NoH2InClasspathTest {

    @Test
    void h2DriverIsNotOnTheDefaultTestClasspath() {
        assertThatThrownBy(() -> Class.forName("org.h2.Driver")).isInstanceOf(ClassNotFoundException.class);
    }
}
