package com.meditrack.patient.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @org.springframework.beans.factory.annotation.Autowired
    private com.meditrack.patient.security.AccessValidationFilter accessValidationFilter;
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(accessValidationFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                // Health endpoints - public access
                .requestMatchers("/actuator/health", "/actuator/info", "/actuator/metrics").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                // FHIR endpoints - public access for healthcare systems
                .requestMatchers("/api/fhir/**").permitAll()
                // Access login endpoint is intentionally public so the gateway can validate logins
                .requestMatchers("/api/access/login").permitAll()
                // Swagger/OpenAPI documentation - public access
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                // Access control is enforced by the request filter and service layer
                .anyRequest().permitAll()
            );
        
        return http.build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        
        org.springframework.web.cors.CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:3000",  // React frontend
            "http://localhost:5173",  // Vite frontend
            "http://127.0.0.1:5173",  // Vite frontend via loopback
            "http://localhost:8090",  // API Gateway
            "http://localhost:8082",  // Patient Service
            "https://yourdomain.com"   // Production domain
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "X-User-Id",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"
        ));
        configuration.setExposedHeaders(Arrays.asList(
            "Access-Control-Allow-Origin",
            "Access-Control-Allow-Credentials",
            "Authorization",
            "Content-Type"
        ));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
