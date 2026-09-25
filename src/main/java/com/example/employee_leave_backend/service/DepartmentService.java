package com.example.employee_leave_backend.service;

import com.example.employee_leave_backend.dto.DepartmentRequest;
import com.example.employee_leave_backend.dto.DepartmentResponse;
import com.example.employee_leave_backend.entity.Department;
import com.example.employee_leave_backend.entity.User;
import com.example.employee_leave_backend.exception.BadRequestException;
import com.example.employee_leave_backend.exception.ResourceNotFoundException;
import com.example.employee_leave_backend.repository.DepartmentRepository;
import com.example.employee_leave_backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;

    public DepartmentService(
            DepartmentRepository departmentRepository,
            UserRepository userRepository) {

        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
    }

        @Transactional
        public DepartmentResponse createDepartment(
            DepartmentRequest request) {

        User manager =null;

        if (request.getManagerId() != null) {
            manager = userRepository.findById(request.getManagerId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException("Manager not found"));
                        if (!"MANAGER".equals(manager.getRole())) {
                                throw new BadRequestException("Selected user is not a manager");
                        }
        }

        Department department = new Department();

        department.setName(request.getName());
        department.setStatus("ACTIVE");
        department.setManager(manager);

        Department savedDepartment =
                departmentRepository.save(department);

                return toResponse(savedDepartment);
        }

        public List<DepartmentResponse> getAllDepartments() {
                return departmentRepository.findAll().stream().map(this::toResponse).toList();
        }

        public DepartmentResponse getDepartmentById(Long id) {
                return toResponse(departmentRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Department not found")));
        }

        public DepartmentResponse updateDepartment(Long id, DepartmentRequest request) {
                Department department = departmentRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
                User manager = null;
                if (request.getManagerId() != null) {
                        manager = userRepository.findById(request.getManagerId())
                                        .orElseThrow(() -> new ResourceNotFoundException("Manager not found"));
                        if (!"MANAGER".equals(manager.getRole())) {
                                throw new BadRequestException("Selected user is not a manager");
                        }
                }
                department.setName(request.getName());
                department.setManager(manager);
                return toResponse(departmentRepository.save(department));
        }

        public DepartmentResponse deactivateDepartment(Long id) {
                Department department = departmentRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
                department.setStatus("INACTIVE");
                return toResponse(departmentRepository.save(department));
        }

        public DepartmentResponse getManagerDepartment(String username) {
                User manager = userRepository.findByUsername(username)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                return toResponse(departmentRepository.findByManagerId(manager.getId())
                                .orElseThrow(() -> new ResourceNotFoundException("Department not found")));
        }

        private DepartmentResponse toResponse(Department department) {
                Long managerId = department.getManager() == null ? null : department.getManager().getId();
                return new DepartmentResponse(department.getId(), department.getName(), department.getStatus(), managerId);
    }
}