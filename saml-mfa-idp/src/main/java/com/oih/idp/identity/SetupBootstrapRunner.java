package com.oih.idp.identity;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class SetupBootstrapRunner implements ApplicationRunner {

    private final SetupService setup;

    public SetupBootstrapRunner(SetupService setup) {
        this.setup = setup;
    }

    @Override
    public void run(ApplicationArguments args) {
        setup.ensureBootstrapToken();
    }
}
