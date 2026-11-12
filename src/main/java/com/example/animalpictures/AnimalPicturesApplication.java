package com.example.animalpictures;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AnimalPicturesApplication {

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(AnimalPicturesApplication.class);
		// Moves to the next free port when 8080 is taken, instead of failing to start
		application.addListeners(new PortFallback());
		application.run(args);
	}

}
