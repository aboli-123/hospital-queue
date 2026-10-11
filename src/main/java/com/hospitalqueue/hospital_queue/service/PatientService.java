package com.hospitalqueue.hospital_queue.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.hospitalqueue.hospital_queue.domain.Patient;
import com.hospitalqueue.hospital_queue.repository.PatientRepository;
import com.hospitalqueue.hospital_queue.web.CreatePatientRequest;

@Service
public class PatientService {
    
    private final PatientRepository patientRepository;

    public PatientService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public Patient create(CreatePatientRequest req) {
       
        return patientRepository.save(new Patient(
                req.name(), req.phone()));
    }

    public List<Patient> findAll() {
        return patientRepository.findAll();
    }

    
}



