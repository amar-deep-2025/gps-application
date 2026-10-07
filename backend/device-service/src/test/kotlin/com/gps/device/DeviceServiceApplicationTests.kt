package com.gps.device

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class DeviceServiceApplicationTests {

	@Test
	fun contextLoads() {
		println("DB_URL = ${System.getenv("DB_URL")}")
	}

}
