package com.meditrack.security;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

public class ServiceJwtHttpInterceptor implements ClientHttpRequestInterceptor {

    private final ServiceJwtUtil serviceJwtUtil;
    private final String sourceService;
    private final String targetService;

    public ServiceJwtHttpInterceptor(ServiceJwtUtil serviceJwtUtil, String sourceService, String targetService) {
        this.serviceJwtUtil = serviceJwtUtil;
        this.sourceService = sourceService;
        this.targetService = targetService;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
            String token = serviceJwtUtil.generateToken(sourceService, targetService);
            request.getHeaders().setBearerAuth(token);
        }
        return execution.execute(request, body);
    }
}
