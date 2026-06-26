package com.meditrack.user.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleCredentials {
    private List<LoginCredentials> admin;
    private List<LoginCredentials> doctor;
    private List<LoginCredentials> nurse;
}
