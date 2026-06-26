package com.meditrack.user.repository;

import com.meditrack.user.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, String> {

    java.util.List<Department> findByIsActive(boolean isActive);
}
