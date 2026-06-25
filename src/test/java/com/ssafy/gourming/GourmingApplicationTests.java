package com.ssafy.gourming;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "password-reset.mail.enabled=false")
class GourmingApplicationTests {

	@Test
	void contextLoads() {
	}

}
