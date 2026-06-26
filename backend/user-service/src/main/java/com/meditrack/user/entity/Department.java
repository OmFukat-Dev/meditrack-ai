package com.meditrack.user.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "departments")
public class Department {
    
    @Id
    private String id;
    
    @Column(nullable = false, unique = true)
    private String departmentName;
    
    @Column
    private String description;

    @Column(nullable = false)
    private boolean isActive = true;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}
