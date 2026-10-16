package com.serveless.envio.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DatabaseUrlEnvironmentPostProcessorTest {

	private final DatabaseUrlEnvironmentPostProcessor processor =
			new DatabaseUrlEnvironmentPostProcessor();

	@Test
	void convertsMysqlUrlToJdbcAndSetsDriver() {
		MockEnvironment environment = new MockEnvironment()
				.withProperty("SPRING_DATASOURCE_URL", "mysql://root:secret@host:33136/railway");

		processor.postProcessEnvironment(environment, null);

		assertEquals(
				"jdbc:mysql://root:secret@host:33136/railway"
						+ "?allowPublicKeyRetrieval=true&useSSL=true&serverTimezone=UTC",
				environment.getProperty("spring.datasource.url"));
		assertEquals("com.mysql.cj.jdbc.Driver",
				environment.getProperty("spring.datasource.driver-class-name"));
	}

	@Test
	void fallsBackToDatabaseUrlWhenSpringDatasourceUrlIsAbsent() {
		MockEnvironment environment = new MockEnvironment()
				.withProperty("DATABASE_URL", "mysql://u:p@host:3306/db");

		processor.postProcessEnvironment(environment, null);

		assertEquals(
				"jdbc:mysql://u:p@host:3306/db"
						+ "?allowPublicKeyRetrieval=true&useSSL=true&serverTimezone=UTC",
				environment.getProperty("spring.datasource.url"));
	}

	@Test
	void leavesAlreadyJdbcUrlUntouched() {
		MockEnvironment environment = new MockEnvironment()
				.withProperty("SPRING_DATASOURCE_URL", "jdbc:mysql://host:3306/db");

		processor.postProcessEnvironment(environment, null);

		assertNull(environment.getProperty("spring.datasource.url"));
	}

	@Test
	void doesNothingWhenNoUrlIsProvided() {
		MockEnvironment environment = new MockEnvironment();

		processor.postProcessEnvironment(environment, null);

		assertNull(environment.getProperty("spring.datasource.url"));
	}
}
