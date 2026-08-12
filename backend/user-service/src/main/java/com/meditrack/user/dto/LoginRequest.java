package com.meditrack.user.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    private String email;
    private String password;
    private String ipAddress;
    private String userAgent;
}
