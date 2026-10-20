package com.hospitalqueue.hospital_queue.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hospitalqueue.hospital_queue.domain.Appointment;
import com.hospitalqueue.hospital_queue.domain.AppointmentStatus;
import com.hospitalqueue.hospital_queue.exception.NotFoundException;
import com.hospitalqueue.hospital_queue.repository.AppointmentRepository;
import com.hospitalqueue.hospital_queue.web.QueueStatusResponse;

@Service
public class QueueService {

    private static final int AVERAGE_WINDOW = 10;        // last 10 completed visits
    private static final double DEFAULT_MINUTES = 10.0;  // used when there is no history yet
    private static final double MIN_MINUTES = 0.0;       // set to 1.0 before real use, to ignore accidental clicks
    private static final double MAX_MINUTES = 90.0;      // ignore forgotten Finish/Call-next clicks

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

    // Average of the doctor's last real consultations, in minutes
    @Transactional(readOnly = true)
    public double averageConsultationMinutes(Long doctorId) {
        List<Appointment> recent = appointmentRepository
                .findByDoctorIdAndStatusOrderByFinishedAtDesc(
                        doctorId, AppointmentStatus.COMPLETED, PageRequest.of(0, AVERAGE_WINDOW));

        return recent.stream()
                .filter(a -> a.getStartedAt() != null && a.getFinishedAt() != null)
                .mapToDouble(a -> Duration.between(a.getStartedAt(), a.getFinishedAt()).toSeconds() / 60.0)
                .filter(m -> m >= MIN_MINUTES && m <= MAX_MINUTES)
                .average()
                .orElse(DEFAULT_MINUTES);
    }

    // What the patient sees: "N patients ahead, about X minutes"
    @Transactional(readOnly = true)
    public QueueStatusResponse statusOf(Long appointmentId) {
        Appointment a = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment " + appointmentId + " not found"));

        boolean inQueue = a.getStatus() == AppointmentStatus.BOOKED
                || a.getStatus() == AppointmentStatus.WAITING;
        if (!inQueue) {
            return new QueueStatusResponse(a.getId(), a.getStatus(), null, null);
        }

        Long doctorId = a.getDoctor().getId();
        List<Long> order = queueFor(doctorId).stream().map(Appointment::getId).toList();

        int ahead = order.indexOf(a.getId());
        if (appointmentRepository.existsByDoctorIdAndStatus(doctorId, AppointmentStatus.IN_PROGRESS)) {
            ahead += 1;   // the patient currently with the doctor is also "ahead"
        }
        long estimate = Math.round(averageConsultationMinutes(doctorId) * ahead);
        return new QueueStatusResponse(a.getId(), a.getStatus(), ahead, estimate);
    }
}