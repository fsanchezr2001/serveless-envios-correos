package com.serveless.envio.config;

import com.sendgrid.SendGrid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class SendGridConfig {

	@Bean
	public SendGrid sendGrid(@Value("${sendgrid.api-key}") String apiKey) {
		if (!StringUtils.hasText(apiKey)) {
			throw new IllegalStateException("SendGrid API key is not configured. Please set SENDGRID_API_KEY environment variable.");
		}
		return new SendGrid(apiKey);
	}
}