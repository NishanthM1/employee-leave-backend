package com.example.employee_leave_backend.security;

import com.example.employee_leave_backend.entity.User;
import com.example.employee_leave_backend.entity.Employee;
import com.example.employee_leave_backend.repository.EmployeeRepository;
import com.example.employee_leave_backend.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;

    public CustomUserDetailsService(
            UserRepository userRepository,
            EmployeeRepository employeeRepository) {
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found"));

        boolean enabled = true;
        if ("EMPLOYEE".equals(user.getRole())) {
            Employee employee = employeeRepository.findByUserId(user.getId())
                .orElseThrow(() -> new UsernameNotFoundException("Employee not found"));
            enabled = "ACTIVE".equals(employee.getStatus());
        }

        return org.springframework.security.core.userdetails.User.withUsername(user.getUsername())
            .password(user.getPassword())
            .roles(user.getRole())
            .disabled(!enabled)
            .build();
    }
}