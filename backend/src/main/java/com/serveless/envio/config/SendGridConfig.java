package com.serveless.envio.config;

import com.sendgrid.SendGrid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class SendGridConfig {

	private static final Logger log = LoggerFactory.getLogger(SendGridConfig.class);

	@Bean
	public SendGrid sendGrid(@Value("${sendgrid.api-key:}") String apiKey,
			@Value("${sendgrid.from-address:}") String fromEmail,
			@Value("${sendgrid.from-name:}") String fromName) {
		if (!StringUtils.hasText(apiKey)) {
			log.warn("SENDGRID_API_KEY is not configured. The app will start but email sending will fail until a real key is provided.");
		}
		if (!StringUtils.hasText(fromEmail)) {
			log.warn("SENDGRID_FROM is not configured. The app will start but email sending will fail until a real sender is provided.");
		}
		return new SendGrid(apiKey);
	}
}