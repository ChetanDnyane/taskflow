package com.chetan.taskflow;

// <editor-fold defaultstate="collapsed" desc="Application startup smoke test">
/*
 * SpringBootTest constructs the real application context. ActiveProfiles(test) selects H2 and the
 * public test signing key so development database credentials are unnecessary. The empty test body
 * is intentional: dependency injection or configuration failures cause context creation to fail
 * before the method runs. This does not exercise any individual HTTP endpoint.
 */
// </editor-fold>

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class TaskflowApplicationTests {

	@Test
	void contextLoads() {
	}

}
