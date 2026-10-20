package com.hospitalqueue.hospital_queue.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hospitalqueue.hospital_queue.domain.Appointment;
import com.hospitalqueue.hospital_queue.domain.AppointmentStatus;
import com.hospitalqueue.hospital_queue.repository.AppointmentRepository;

@Service
public class QueueService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorService doctorService;

    public QueueService(AppointmentRepository appointmentRepository, DoctorService doctorService) {
        this.appointmentRepository = appointmentRepository;
        this.doctorService = doctorService;
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

    @Transactional
    public Optional<Appointment> callNext(Long doctorId) {
        doctorService.get(doctorId);               // 404 if the doctor doesn't exist
        LocalDateTime now = LocalDateTime.now();

        // 1. finish the patient who is with the doctor now
        appointmentRepository.findFirstByDoctorIdAndStatus(doctorId, AppointmentStatus.IN_PROGRESS)
                .ifPresent(current -> {
                    current.setStatus(AppointmentStatus.COMPLETED);
                    current.setFinishedAt(now);
                });

        // 2. start the first patient who is actually present (WAITING)
        Optional<Appointment> next = queueFor(doctorId).stream()
                .filter(a -> a.getStatus() == AppointmentStatus.WAITING)
                .findFirst();

        next.ifPresent(a -> {
            a.setStatus(AppointmentStatus.IN_PROGRESS);
            a.setStartedAt(now);
        });
        return next;
    }
}