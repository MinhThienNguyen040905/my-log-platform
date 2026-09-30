package com.mylog;

import org.springframework.boot.SpringApplication;

public class TestMylogApplication {

	public static void main(String[] args) {
		SpringApplication.from(MylogApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
