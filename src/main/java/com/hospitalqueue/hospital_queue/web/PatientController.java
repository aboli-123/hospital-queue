package com.hospitalqueue.hospital_queue.web;


import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.hospitalqueue.hospital_queue.domain.Patient;
import com.hospitalqueue.hospital_queue.service.PatientService;


import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/patients")

public class PatientController {

      private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Patient create(@Valid @RequestBody CreatePatientRequest req) {
        return patientService.create(req);
    }

    @GetMapping
    public List<Patient> list() {
        return patientService.findAll();
    }


    
}


