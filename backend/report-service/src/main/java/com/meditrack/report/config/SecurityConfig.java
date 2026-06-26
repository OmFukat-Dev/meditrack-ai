package com.meditrack.report.config;

import com.meditrack.security.MeditrackJwtUtil;
import com.meditrack.security.MeditrackSecurityProperties;
import com.meditrack.security.RequestAuthenticationFilter;
import com.meditrack.security.ServiceJwtUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public RequestAuthenticationFilter requestAuthenticationFilter(
        MeditrackSecurityProperties properties,
        MeditrackJwtUtil jwtUtil,
        ServiceJwtUtil serviceJwtUtil
    ) {
        return new RequestAuthenticationFilter(properties, jwtUtil, serviceJwtUtil, "report-service");
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, RequestAuthenticationFilter requestAuthenticationFilter) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(requestAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .anyRequest().authenticated()
            );

        return http.build();
    }
}
