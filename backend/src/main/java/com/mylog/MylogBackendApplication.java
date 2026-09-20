package com.mylog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MylogBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(MylogBackendApplication.class, args);
	}

}
