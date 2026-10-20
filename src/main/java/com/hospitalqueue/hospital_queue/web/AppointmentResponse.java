package com.hospitalqueue.hospital_queue.web;

import java.time.LocalDateTime;

import com.hospitalqueue.hospital_queue.domain.Appointment;
import com.hospitalqueue.hospital_queue.domain.AppointmentStatus;
import com.hospitalqueue.hospital_queue.domain.Priority;

public record AppointmentResponse(
        Long id,
        Long doctorId,
        String doctorName,
        Long patientId,
        String patientName,
        LocalDateTime slotTime,
        AppointmentStatus status,
        Priority priority,
        LocalDateTime arrivedAt,
        LocalDateTime startedAt,
        LocalDateTime finishedAt) {

    public static AppointmentResponse from(Appointment a) {
        return new AppointmentResponse(
                a.getId(),
                a.getDoctor().getId(), a.getDoctor().getName(),
                a.getPatient().getId(), a.getPatient().getName(),
                a.getSlotTime(), a.getStatus(), a.getPriority(),
                a.getArrivedAt(), a.getStartedAt(), a.getFinishedAt());
    }
}