package com.xieguotao.face;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FaceApplication {

	public static void main(String[] args) {
		SpringApplication.run(FaceApplication.class, args);
	}

}
