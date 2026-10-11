package com.hospitalqueue.hospital_queue.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.hospitalqueue.hospital_queue.domain.Doctor;
import com.hospitalqueue.hospital_queue.repository.DoctorRepository;
import com.hospitalqueue.hospital_queue.web.CreateDoctorRequest;
import com.hospitalqueue.hospital_queue.exception.NotFoundException;

@Service
public class DoctorService {

    private final DoctorRepository doctorRepository;

    public DoctorService(DoctorRepository doctorRepository) {
        this.doctorRepository = doctorRepository;
    }

    public Doctor create(CreateDoctorRequest req) {
        if (!req.workEnd().isAfter(req.workStart())) {
            throw new IllegalArgumentException("workEnd must be after workStart");
        }
        return doctorRepository.save(new Doctor(
                req.name(), req.department(), req.workStart(), req.workEnd(), req.slotMinutes()));
    }

    public List<Doctor> findAll() {
        return doctorRepository.findAll();
    }

    public Doctor get(Long id) {
        return doctorRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Doctor " + id + " not found"));
    }
}
