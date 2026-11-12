package com.example.animalpictures;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AnimalPicturesApplication {

	public static void main(String[] args) {
		SpringApplication.run(AnimalPicturesApplication.class, args);
	}

}
