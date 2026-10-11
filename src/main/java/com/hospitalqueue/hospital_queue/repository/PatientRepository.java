package com.hospitalqueue.hospital_queue.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.hospitalqueue.hospital_queue.domain.Patient;

public interface PatientRepository extends JpaRepository<Patient, Long> {
}