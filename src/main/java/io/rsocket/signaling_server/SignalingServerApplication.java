package io.rsocket.signaling_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "io.rsocket")
public class SignalingServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(SignalingServerApplication.class, args);
	}

}
