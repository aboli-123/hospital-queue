package com.hospitalqueue.hospital_queue.domain;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "appointments", uniqueConstraints = @UniqueConstraint(
        name = "uk_doctor_active_slot", columnNames = { "doctor_id", "active_slot" }))
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "doctor_id")
    private Doctor doctor;

    @ManyToOne(optional = false)
    @JoinColumn(name = "patient_id")
    private Patient patient;

    @Column(name = "slot_time")
    private LocalDateTime slotTime;

    // Copy of slotTime while the appointment holds the slot; null after cancelling.
    // The unique rule uses this column, so a cancelled slot can be booked again.
    @Column(name = "active_slot")
    private LocalDateTime activeSlot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status = AppointmentStatus.BOOKED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority = Priority.NORMAL;

    @Column(name = "arrived_at")
    private LocalDateTime arrivedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    // Hibernate uses this one
    protected Appointment() {
    }

    // We use this one when booking
    public Appointment(Doctor doctor, Patient patient, LocalDateTime slotTime, Priority priority) {
        this.doctor = doctor;
        this.patient = patient;
        this.slotTime = slotTime;
        this.activeSlot = slotTime;
        this.priority = priority;
    }

    // Cancelling frees the slot and keeps the row for history
    public void cancel() {
        this.status = AppointmentStatus.CANCELLED;
        this.activeSlot = null;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public Doctor getDoctor() {
        return doctor;
    }

    public Patient getPatient() {
        return patient;
    }

    public LocalDateTime getSlotTime() {
        return slotTime;
    }

    public LocalDateTime getActiveSlot() {
        return activeSlot;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public Priority getPriority() {
        return priority;
    }

    public LocalDateTime getArrivedAt() {
        return arrivedAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    // Setters (only the fields that change while the patient moves through the queue)
    public void setStatus(AppointmentStatus status) {
        this.status = status;
    }

    public void setArrivedAt(LocalDateTime arrivedAt) {
        this.arrivedAt = arrivedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }
}