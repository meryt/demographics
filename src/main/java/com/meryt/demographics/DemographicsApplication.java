package com.meryt.demographics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableAutoConfiguration
@SpringBootApplication
public class DemographicsApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemographicsApplication.class, args);
	}
}
