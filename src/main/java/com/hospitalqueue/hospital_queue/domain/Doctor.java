package com.hospitalqueue.hospital_queue.domain;

import jakarta.persistence.*;
import java.time.LocalTime;

@Entity
@Table(name = "doctors")
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String department;
    private LocalTime workStart;
    private LocalTime workEnd;
    private int slotMinutes;

    protected Doctor() {}

    public Doctor(String name, String department, LocalTime workStart, LocalTime workEnd, int slotMinutes) {
        this.name = name;
        this.department = department;
        this.workStart = workStart;
        this.workEnd = workEnd;
        this.slotMinutes = slotMinutes;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDepartment() {
        return department;
    }

    public LocalTime getWorkStart() {
        return workStart;
    }

    public LocalTime getWorkEnd() {
        return workEnd;
    }

    public int getSlotMinutes() {
        return slotMinutes;
    }   
    
}