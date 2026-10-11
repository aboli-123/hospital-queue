package com.hospitalqueue.hospital_queue.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hospitalqueue.hospital_queue.domain.Appointment;
import com.hospitalqueue.hospital_queue.domain.AppointmentStatus;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    // Is this slot already taken for this doctor?
    boolean existsByDoctorIdAndSlotTime(Long doctorId, LocalDateTime slotTime);

    // All patients of a doctor with a given status (e.g. everyone WAITING = the queue)
    List<Appointment> findByDoctorIdAndStatus(Long doctorId, AppointmentStatus status);

    // Is the doctor currently busy? (any IN_PROGRESS appointment)
    boolean existsByDoctorIdAndStatus(Long doctorId, AppointmentStatus status);
}