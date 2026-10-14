package com.hospitalqueue.hospital_queue.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hospitalqueue.hospital_queue.domain.Appointment;
import com.hospitalqueue.hospital_queue.domain.AppointmentStatus;
import com.hospitalqueue.hospital_queue.repository.AppointmentRepository;

@Service
public class QueueService {

    private final AppointmentRepository appointmentRepository;

    public QueueService(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    // Everyone still to be seen: at home (BOOKED) and at the clinic (WAITING)
    @Transactional(readOnly = true)
    public List<Appointment> queueFor(Long doctorId) {
        return appointmentRepository
                .findByDoctorIdAndStatusIn(doctorId,
                        List.of(AppointmentStatus.BOOKED, AppointmentStatus.WAITING))
                .stream()
                .sorted(Comparator
                        .comparing(Appointment::getPriority)   // EMERGENCY before NORMAL
                        .thenComparing(this::effectiveTime))   // then by effective time
                .toList();
    }

    // The later of slot time and arrival time, so a late patient goes behind on-time patients
    private LocalDateTime effectiveTime(Appointment a) {
        if (a.getArrivedAt() == null) {
            return a.getSlotTime();
        }
        if (a.getSlotTime() == null) {
            return a.getArrivedAt();
        }
        return a.getSlotTime().isAfter(a.getArrivedAt())
                ? a.getSlotTime()
                : a.getArrivedAt();
    }
}