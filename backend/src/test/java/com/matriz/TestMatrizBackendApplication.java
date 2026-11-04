package com.matriz;

import org.springframework.boot.SpringApplication;

public class TestMatrizBackendApplication {

	public static void main(String[] args) {
		SpringApplication.from(MatrizBackendApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
