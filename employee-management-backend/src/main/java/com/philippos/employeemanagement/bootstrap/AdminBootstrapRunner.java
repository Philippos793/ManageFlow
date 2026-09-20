package com.philippos.employeemanagement.bootstrap;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private final AdminBootstrapService adminBootstrapService;
    private final Environment environment;

    public AdminBootstrapRunner(
            AdminBootstrapService adminBootstrapService,
            Environment environment) {
        this.adminBootstrapService = adminBootstrapService;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        adminBootstrapService.createInitialAdminIfRequired(
                environment.getProperty("ADMIN_USERNAME"),
                environment.getProperty("ADMIN_PASSWORD"));
    }
}
