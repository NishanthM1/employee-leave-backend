package com.example.employee_leave_backend.repository;

import com.example.employee_leave_backend.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

	Optional<Department> findByManagerId(Long managerId);
}