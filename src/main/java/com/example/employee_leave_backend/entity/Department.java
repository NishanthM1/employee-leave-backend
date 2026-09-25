package com.example.employee_leave_backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "departments")
public class Department {

    @Id
    @GeneratedValue
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    private String status;

    @ManyToOne
    @JoinColumn(name = "manager_id")
    private User manager;

    // Constructors
    public Department() {}
    public Department(Long id, String name, String status, User manager) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.manager = manager;
    }

    // Getters & Setters

    public Long getId() { return id; }

    public void setId(Long id) {    this.id = id;   }

    public String getName() {   return name;    }

    public void setName(String name) {  this.name = name;   }

    public String getStatus() {    return status;     }

    public void setStatus(String status) {  this.status = status;   }

    public User getManager() {  return manager;}

    public void setManager(User manager) {  this.manager = manager; }
}