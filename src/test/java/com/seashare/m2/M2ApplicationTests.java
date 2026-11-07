package com.seashare.m2;

import com.seashare.m2.shared.PostgresTestContainers;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(PostgresTestContainers.class)
class M2ApplicationTests {

	@Test
	void contextLoads() {
	}

}
