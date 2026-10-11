package com.hospitalqueue.hospital_queue.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.hospitalqueue.hospital_queue.domain.Doctor;
import com.hospitalqueue.hospital_queue.service.DoctorService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/doctors")
public class DoctorController {

    private final DoctorService doctorService;

    public DoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Doctor create(@Valid @RequestBody CreateDoctorRequest req) {
        return doctorService.create(req);
    }

    @GetMapping
    public List<Doctor> list() {
        return doctorService.findAll();
    }
}