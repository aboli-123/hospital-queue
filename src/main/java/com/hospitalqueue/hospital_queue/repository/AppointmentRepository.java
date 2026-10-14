package com.hospitalqueue.hospital_queue.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hospitalqueue.hospital_queue.domain.Appointment;
import com.hospitalqueue.hospital_queue.domain.AppointmentStatus;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    boolean existsByDoctorIdAndSlotTime(Long doctorId, LocalDateTime slotTime);

    List<Appointment> findByDoctorIdAndStatus(Long doctorId, AppointmentStatus status);

    boolean existsByDoctorIdAndStatus(Long doctorId, AppointmentStatus status);

    // All appointments of a doctor whose status is any one of the given statuses
    List<Appointment> findByDoctorIdAndStatusIn(Long doctorId, Collection<AppointmentStatus> statuses);
}