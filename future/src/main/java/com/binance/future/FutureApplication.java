package com.binance.future;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

@SpringBootApplication(exclude = {SecurityAutoConfiguration.class })
public class FutureApplication {

	public static void main(String[] args) {
		SpringApplication.run(FutureApplication.class, args);
	}

}
