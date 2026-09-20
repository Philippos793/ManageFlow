package com.philippos.employeemanagement;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.philippos.employeemanagement.bootstrap.AdminBootstrapService;

@SpringBootTest
class EmployeeManagementBackendApplicationTests {

	@MockitoBean
	private AdminBootstrapService adminBootstrapService;

	@Test
	void contextLoads() {
	}

}
