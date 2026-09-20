package com.philippos.employeemanagement.bootstrap;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.mock.env.MockEnvironment;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AdminBootstrapRunnerTest {

    @Test
    void passesEnvironmentCredentialsToBootstrapService() throws Exception {
        AdminBootstrapService service = mock(AdminBootstrapService.class);
        MockEnvironment environment = new MockEnvironment()
                .withProperty("ADMIN_USERNAME", "initial.admin")
                .withProperty("ADMIN_PASSWORD", "SecurePassword123");
        AdminBootstrapRunner runner = new AdminBootstrapRunner(service, environment);

        runner.run(new DefaultApplicationArguments());

        verify(service).createInitialAdminIfRequired(
                "initial.admin", "SecurePassword123");
    }
}
