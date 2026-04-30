package com.byteentropy.webhook_core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.byteentropy.webhook_core.config.WebhookProperties;

@SpringBootApplication
@EnableConfigurationProperties(WebhookProperties.class)
public class WebhookCoreApplication {

	public static void main(String[] args) {
		SpringApplication.run(WebhookCoreApplication.class, args);
	}

}
