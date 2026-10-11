package com.hospitalqueue.hospital_queue.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.hospitalqueue.hospital_queue.domain.Doctor;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {
}