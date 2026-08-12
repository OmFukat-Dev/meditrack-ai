package com.meditrack.patient.controller;

import com.meditrack.patient.dto.AccessLoginRequest;
import com.meditrack.patient.dto.AccessPrincipalResponse;
import com.meditrack.patient.service.StaffService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/access")
public class AccessController {

    private final StaffService staffService;

    public AccessController(StaffService staffService) {
        this.staffService = staffService;
    }

    @PostMapping("/login")
    public ResponseEntity<AccessPrincipalResponse> login(@RequestBody AccessLoginRequest request) {
        return ResponseEntity.ok(staffService.authenticate(request));
    }
}
