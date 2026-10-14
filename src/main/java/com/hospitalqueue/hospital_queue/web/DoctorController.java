package com.hospitalqueue.hospital_queue.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.hospitalqueue.hospital_queue.domain.Doctor;
import com.hospitalqueue.hospital_queue.service.DoctorService;
import com.hospitalqueue.hospital_queue.service.QueueService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/doctors")
public class DoctorController {

    private final DoctorService doctorService;
    private final QueueService queueService;

    public DoctorController(DoctorService doctorService, QueueService queueService) {
        this.doctorService = doctorService;
        this.queueService = queueService;
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

    @GetMapping("/{id}/queue")
    public List<AppointmentResponse> queue(@PathVariable Long id) {
        return queueService.queueFor(id).stream()
                .map(AppointmentResponse::from)
                .toList();
    }
}