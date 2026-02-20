package com.ellipsis.homelabmonitor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HomelabMonitorApplication {

	public static void main(String[] args) {
        SpringApplication.run(HomelabMonitorApplication.class, args);
	}
}
