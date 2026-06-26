package com.meditrack.patient.controller;

import com.meditrack.patient.dto.StaffMemberRequest;
import com.meditrack.patient.dto.StaffMemberResponse;
import com.meditrack.patient.service.StaffService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/staff-members")
@CrossOrigin(origins = "*")
public class StaffMemberController {

    private final StaffService staffService;

    public StaffMemberController(StaffService staffService) {
        this.staffService = staffService;
    }

    @GetMapping
    public ResponseEntity<List<StaffMemberResponse>> listStaffMembers(@RequestParam(required = false) String role) {
        return ResponseEntity.ok(staffService.getStaffMembers(role));
    }

    @GetMapping("/doctors")
    public ResponseEntity<List<StaffMemberResponse>> listDoctors() {
        return ResponseEntity.ok(staffService.getSeededDoctors());
    }

    @GetMapping("/nurses")
    public ResponseEntity<List<StaffMemberResponse>> listNurses() {
        return ResponseEntity.ok(staffService.getSeededNurses());
    }

    @PostMapping
    public ResponseEntity<StaffMemberResponse> createStaffMember(@RequestBody StaffMemberRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(staffService.createStaffMember(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StaffMemberResponse> updateStaffMember(@PathVariable Long id, @RequestBody StaffMemberRequest request) {
        return ResponseEntity.ok(staffService.updateStaffMember(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateStaffMember(@PathVariable Long id) {
        staffService.deactivateStaffMember(id);
        return ResponseEntity.noContent().build();
    }
}
