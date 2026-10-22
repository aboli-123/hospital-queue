package com.hospitalqueue.hospital_queue.service;

import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hospitalqueue.hospital_queue.domain.Appointment;
import com.hospitalqueue.hospital_queue.domain.AppointmentStatus;
import com.hospitalqueue.hospital_queue.domain.Doctor;
import com.hospitalqueue.hospital_queue.domain.Patient;
import com.hospitalqueue.hospital_queue.domain.Priority;
import com.hospitalqueue.hospital_queue.exception.InvalidStateException;
import com.hospitalqueue.hospital_queue.exception.NotFoundException;
import com.hospitalqueue.hospital_queue.exception.SlotUnavailableException;
import com.hospitalqueue.hospital_queue.repository.AppointmentRepository;
import com.hospitalqueue.hospital_queue.web.BookingRequest;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorService doctorService;
    private final PatientService patientService;

    public AppointmentService(AppointmentRepository appointmentRepository,
            DoctorService doctorService,
            PatientService patientService) {
        this.appointmentRepository = appointmentRepository;
        this.doctorService = doctorService;
        this.patientService = patientService;
    }

    @Transactional
    public Appointment book(BookingRequest req) {

        Doctor doctor = doctorService.get(req.doctorId());
        Patient patient = patientService.get(req.patientId());
        LocalDateTime slot = req.slotTime();

        validateSlot(doctor, slot);

        if (appointmentRepository.existsByDoctorIdAndActiveSlot(doctor.getId(), slot)) {
            throw new SlotUnavailableException("Slot " + slot + " is already booked");
        }
        try {
            return appointmentRepository.saveAndFlush(
                    new Appointment(doctor, patient, slot, Priority.NORMAL));
        } catch (DataIntegrityViolationException e) {
            throw new SlotUnavailableException("Slot " + slot + " was just taken by someone else");
        }
    }

    @Transactional
    public Appointment markArrived(Long id) {
        Appointment a = get(id);

        if (a.getStatus() != AppointmentStatus.BOOKED) {
            throw new InvalidStateException("Cannot mark arrived: appointment is " + a.getStatus());
        }
        a.setStatus(AppointmentStatus.WAITING);
        a.setArrivedAt(LocalDateTime.now());
        return a;
    }

    @Transactional
    public Appointment cancel(Long id) {
        Appointment a = get(id);

        if (a.getStatus() != AppointmentStatus.BOOKED && a.getStatus() != AppointmentStatus.WAITING) {
            throw new InvalidStateException("Cannot cancel: appointment is " + a.getStatus());
        }
        a.cancel();
        return a;
    }

    @Transactional(readOnly = true)
    public Appointment get(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Appointment " + id + " not found"));
    }

    private void validateSlot(Doctor doctor, LocalDateTime slot) {
        if (!slot.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Slot must be in the future");
        }
        LocalDateTime dayStart = slot.toLocalDate().atTime(doctor.getWorkStart());
        LocalDateTime dayEnd = slot.toLocalDate().atTime(doctor.getWorkEnd());
        long minutesFromStart = Duration.between(dayStart, slot).toMinutes();

        if (slot.isBefore(dayStart)) {
            throw new IllegalArgumentException("Slot is before the doctor's working hours");
        }
        if (slot.plusMinutes(doctor.getSlotMinutes()).isAfter(dayEnd)) {
            throw new IllegalArgumentException("Slot ends after the doctor's working hours");
        }
        if (minutesFromStart % doctor.getSlotMinutes() != 0 || slot.getSecond() != 0) {
            throw new IllegalArgumentException("Slot must match the doctor's slot grid, for example 10:00, 10:15");
        }
    }
}