package com.lms.auth_service;

import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
// import com.lms.auth_service.entity.User;
// import com.lms.auth_service.enums.ApprovalStatus;
// import com.lms.auth_service.enums.AuthProvider;
// import com.lms.auth_service.enums.Role;
// import com.lms.auth_service.repository.UserRepository;
// import org.springframework.security.crypto.password.PasswordEncoder;
// import java.time.LocalDateTime;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableDiscoveryClient
public class AuthServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuthServiceApplication.class, args);
	}

	@Bean
	@LoadBalanced
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

}
