package com.hospitalqueue.hospital_queue.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.hospitalqueue.hospital_queue.domain.Appointment;
import com.hospitalqueue.hospital_queue.domain.AppointmentStatus;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    boolean existsByDoctorIdAndSlotTime(Long doctorId, LocalDateTime slotTime);

    List<Appointment> findByDoctorIdAndStatus(Long doctorId, AppointmentStatus status);

    boolean existsByDoctorIdAndStatus(Long doctorId, AppointmentStatus status);

    List<Appointment> findByDoctorIdAndStatusIn(Long doctorId, Collection<AppointmentStatus> statuses);

    Optional<Appointment> findFirstByDoctorIdAndStatus(Long doctorId, AppointmentStatus status);

    // Most recently finished first. Pass PageRequest.of(0, 10) to get the last 10.
    List<Appointment> findByDoctorIdAndStatusOrderByFinishedAtDesc(
            Long doctorId, AppointmentStatus status, Pageable pageable);
}