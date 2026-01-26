package com.ZyroTrade;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ZyroTradeApplication {

	public static void main(String[] args) {
		SpringApplication.run(ZyroTradeApplication.class, args);
	}

}
