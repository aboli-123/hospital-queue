package com.hospitalqueue.hospital_queue.web;

import com.hospitalqueue.hospital_queue.domain.AppointmentStatus;

// patientsAhead and estimatedWaitMinutes are null when the patient is not in the queue
// (for example already COMPLETED or IN_PROGRESS).
public record QueueStatusResponse(
        Long appointmentId,
        AppointmentStatus status,
        Integer patientsAhead,
        Long estimatedWaitMinutes) {
}