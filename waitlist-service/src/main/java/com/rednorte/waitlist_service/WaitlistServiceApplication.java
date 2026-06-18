package com.rednorte.waitlist_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class WaitlistServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(WaitlistServiceApplication.class, args);
	}

}
