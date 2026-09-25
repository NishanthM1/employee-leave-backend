package com.example.employee_leave_backend.config;

import com.example.employee_leave_backend.entity.User;
import com.example.employee_leave_backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile("dev")
public class DataInitializer {

    @Bean
    CommandLineRunner createTestUsers(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {

            if (userRepository.findByUsername("admin1").isEmpty()) {

                User admin = new User();

                admin.setUsername("admin1");
                admin.setPassword(
                        passwordEncoder.encode("123456")
                );
                admin.setRole("ADMIN");

                userRepository.save(admin);

                System.out.println("Admin user created");
            }


        };
    }
}