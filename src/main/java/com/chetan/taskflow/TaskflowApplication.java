package com.chetan.taskflow;

// <editor-fold defaultstate="collapsed" desc="Application entry point">
/*
 * SpringApplication.run creates the Spring container and starts the embedded web server.
 * SpringBootApplication enables configuration, auto-configuration and component scanning.
 * Because this class is in the root package, controllers, services, repositories and security
 * configuration in its subpackages are discovered. Runtime settings come from application.properties.
 */
// </editor-fold>

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TaskflowApplication {

	public static void main(String[] args) {
		SpringApplication.run(TaskflowApplication.class, args);
	}

}
