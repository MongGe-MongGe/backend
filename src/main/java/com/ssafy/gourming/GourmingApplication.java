package com.ssafy.gourming;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class GourmingApplication {

	public static void main(String[] args) {
		SpringApplication.run(GourmingApplication.class, args);
	}

}
