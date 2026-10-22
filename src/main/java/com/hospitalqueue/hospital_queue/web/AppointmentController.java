package com.hospitalqueue.hospital_queue.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.hospitalqueue.hospital_queue.service.AppointmentService;
import com.hospitalqueue.hospital_queue.service.QueueService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final QueueService queueService;

    public AppointmentController(AppointmentService appointmentService, QueueService queueService) {
        this.appointmentService = appointmentService;
        this.queueService = queueService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentResponse book(@Valid @RequestBody BookingRequest req) {
        return AppointmentResponse.from(appointmentService.book(req));
    }

    @GetMapping("/{id}")
    public AppointmentResponse get(@PathVariable Long id) {
        return AppointmentResponse.from(appointmentService.get(id));
    }

    @PostMapping("/{id}/arrive")
    public AppointmentResponse arrive(@PathVariable Long id) {
        return AppointmentResponse.from(appointmentService.markArrived(id));
    }

        @PostMapping("/{id}/cancel")
    public AppointmentResponse cancel(@PathVariable Long id) {
        return AppointmentResponse.from(appointmentService.cancel(id));
    }

    @GetMapping("/{id}/queue-status")
    public QueueStatusResponse queueStatus(@PathVariable Long id) {
        return queueService.statusOf(id);
    }
}