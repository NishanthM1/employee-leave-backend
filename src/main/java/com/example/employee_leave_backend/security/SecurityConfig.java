package com.example.employee_leave_backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
        private final String frontendUrl;

        public SecurityConfig(
                        JwtAuthenticationFilter jwtAuthenticationFilter,
                        @Value("${FRONTEND_URL:http://localhost:3000}") String frontendUrl) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
                this.frontendUrl = frontendUrl;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/employees", "/api/departments", "/api/leave-types", "/api/leave-balances").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/employees/*", "/api/leave-types/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/employees/*/deactivate", "/api/leave-types/*/deactivate").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/leave-balances/me").hasRole("EMPLOYEE")
                        .requestMatchers(HttpMethod.GET, "/api/leave-balances", "/api/leave-balances/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/employees").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.GET, "/api/employees/*").hasAnyRole("ADMIN", "MANAGER", "EMPLOYEE")
                        .requestMatchers(HttpMethod.PATCH, "/api/leave-requests/*/approve", "/api/leave-requests/*/reject").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/leave-requests/*/cancel").hasRole("EMPLOYEE")
                        .requestMatchers("/api/leave-requests/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/departments/**", "/api/leave-types/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/departments/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/departments/*/deactivate").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedOrigins(List.of(frontendUrl));
                configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
                configuration.setAllowCredentials(false);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }
}