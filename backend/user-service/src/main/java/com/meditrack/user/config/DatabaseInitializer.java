package com.meditrack.user.config;

import com.meditrack.user.entity.Department;
import com.meditrack.user.entity.Role;
import com.meditrack.user.entity.User;
import com.meditrack.user.repository.DepartmentRepository;
import com.meditrack.user.repository.RoleRepository;
import com.meditrack.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        logger.info("Checking database seeding status...");

        // 1. Seed Roles
        if (roleRepository.count() == 0) {
            logger.info("Seeding roles...");
            createRole("1", "ADMIN", "System administrator with full access");
            createRole("2", "DOCTOR", "Medical doctor with patient management access");
            createRole("3", "NURSE", "Nurse with vitals input and patient care access");
            createRole("4", "VIEWER", "Read-only access for viewing purposes");
        }

        // 2. Seed Departments
        if (departmentRepository.count() == 0) {
            logger.info("Seeding departments...");
            createDepartment("1", "Cardiology", "Heart and cardiovascular system diagnosis and treatment");
            createDepartment("2", "Neurology", "Brain and nervous system diagnosis and treatment");
            createDepartment("3", "Oncology", "Cancer diagnosis and treatment");
            createDepartment("4", "Emergency", "Emergency medical care and trauma treatment");
            createDepartment("5", "ICU", "Intensive Care Unit for critical patients");
            createDepartment("6", "General Medicine", "General medical care and primary treatment");
            createDepartment("7", "Pediatrics", "Medical care for children and infants");
            createDepartment("8", "Orthopedics", "Bone and joint treatment and surgery");
        }

        // 3. Seed Users
        if (userRepository.count() == 0) {
            logger.info("Seeding users...");
            String defaultPasswordHash = passwordEncoder.encode("password123");

            createUser("admin-1", "om@meditrackadmin.ai", defaultPasswordHash, "Om", "Sharma", "1", "1");
            createUser("admin-2", "sakshi@meditrackadmin.ai", defaultPasswordHash, "Sakshi", "Patel", "1", "1");
            createUser("doc-1", "dipanshu@meditrackcardiology.ai", defaultPasswordHash, "Dipanshu", "Sharma", "2", "1");
            createUser("nurse-1", "sarah@meditrackcardiology.ai", defaultPasswordHash, "Sarah", "Johnson", "3", "1");
        }

        logger.info("Database seeding check complete.");
    }

    private void createRole(String id, String roleName, String description) {
        Role role = new Role();
        role.setId(id);
        role.setRoleName(roleName);
        role.setDescription(description);
        roleRepository.save(role);
    }

    private void createDepartment(String id, String name, String description) {
        Department dept = new Department();
        dept.setId(id);
        dept.setDepartmentName(name);
        dept.setDescription(description);
        dept.setActive(true);
        departmentRepository.save(dept);
    }

    private void createUser(String id, String email, String passwordHash, String firstName, String lastName, String roleId, String departmentId) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRoleId(roleId);
        user.setDepartmentId(departmentId);
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        userRepository.save(user);
    }
}
