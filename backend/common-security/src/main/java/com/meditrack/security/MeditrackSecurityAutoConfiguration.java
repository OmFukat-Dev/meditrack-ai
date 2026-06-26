package com.meditrack.security;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(MeditrackSecurityProperties.class)
public class MeditrackSecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MeditrackJwtUtil meditrackJwtUtil(MeditrackSecurityProperties properties) {
        return new MeditrackJwtUtil(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public ServiceJwtUtil serviceJwtUtil(MeditrackSecurityProperties properties) {
        return new ServiceJwtUtil(properties);
    }
}
