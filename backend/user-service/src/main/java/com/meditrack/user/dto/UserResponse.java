package com.meditrack.user.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private String roleId;
    private String departmentId;
    private String roleName;
    private String departmentName;
    private boolean isActive;
    private LocalDateTime lastLogin;
}
