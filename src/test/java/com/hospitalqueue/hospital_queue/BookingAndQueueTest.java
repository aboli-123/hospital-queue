package com.hospitalqueue.hospital_queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.hospitalqueue.hospital_queue.domain.Appointment;
import com.hospitalqueue.hospital_queue.domain.AppointmentStatus;
import com.hospitalqueue.hospital_queue.domain.Doctor;
import com.hospitalqueue.hospital_queue.domain.Patient;
import com.hospitalqueue.hospital_queue.exception.InvalidStateException;
import com.hospitalqueue.hospital_queue.exception.NotFoundException;
import com.hospitalqueue.hospital_queue.exception.SlotUnavailableException;
import com.hospitalqueue.hospital_queue.service.AppointmentService;
import com.hospitalqueue.hospital_queue.service.DoctorService;
import com.hospitalqueue.hospital_queue.service.PatientService;
import com.hospitalqueue.hospital_queue.service.QueueService;
import com.hospitalqueue.hospital_queue.web.BookingRequest;
import com.hospitalqueue.hospital_queue.web.CreateDoctorRequest;
import com.hospitalqueue.hospital_queue.web.CreatePatientRequest;
import com.hospitalqueue.hospital_queue.web.QueueStatusResponse;

@SpringBootTest
@Transactional   // every test runs in a transaction that is rolled back, so tests never leave data behind
class BookingAndQueueTest {

    @Autowired DoctorService doctorService;
    @Autowired PatientService patientService;
    @Autowired AppointmentService appointmentService;
    @Autowired QueueService queueService;

    Doctor doctor;
    Patient ravi;
    Patient anita;
    Patient sam;

    @BeforeEach
    void setUp() {
        doctor = doctorService.create(new CreateDoctorRequest(
                "Dr. Test", "General", LocalTime.of(9, 0), LocalTime.of(13, 0), 15));
        ravi = patientService.create(new CreatePatientRequest("Ravi", "9000000001"));
        anita = patientService.create(new CreatePatientRequest("Anita", "9000000002"));
        sam = patientService.create(new CreatePatientRequest("Sam", "9000000003"));
    }

    // 3 days from today, so the slot is always in the future
    private LocalDateTime at(int hour, int minute) {
        return LocalDate.now().plusDays(3).atTime(hour, minute);
    }

    private Appointment book(Patient patient, LocalDateTime slot) {
        return appointmentService.book(new BookingRequest(doctor.getId(), patient.getId(), slot));
    }

    // ---------- Booking rules ----------

    @Test
    void sameSlotCannotBeBookedTwice() {
        book(ravi, at(10, 0));
        assertThrows(SlotUnavailableException.class, () -> book(anita, at(10, 0)));
    }

    @Test
    void slotOffTheGridIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> book(ravi, at(10, 7)));
    }

    @Test
    void slotOutsideWorkingHoursIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> book(ravi, at(8, 45)));   // before start
        assertThrows(IllegalArgumentException.class, () -> book(ravi, at(13, 0)));   // after the last slot
    }

    @Test
    void slotInThePastIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> book(ravi, LocalDateTime.now().minusDays(1)));
    }

    @Test
    void unknownDoctorGivesNotFound() {
        assertThrows(NotFoundException.class, () -> appointmentService.book(
                new BookingRequest(999L, ravi.getId(), at(10, 0))));
    }

    // ---------- Arrived ----------

    @Test
    void arrivedChangesStatusAndSavesTime() {
        Appointment a = book(ravi, at(10, 0));
        appointmentService.markArrived(a.getId());
        assertEquals(AppointmentStatus.WAITING, a.getStatus());
        assertNotNull(a.getArrivedAt());
    }

    @Test
    void arrivedTwiceIsRefused() {
        Appointment a = book(ravi, at(10, 0));
        appointmentService.markArrived(a.getId());
        assertThrows(InvalidStateException.class, () -> appointmentService.markArrived(a.getId()));
    }

    // ---------- Queue order ----------

    @Test
    void queueIsSortedBySlotTimeNotBookingOrder() {
        Appointment samAppt = book(sam, at(10, 30));      // booked first
        Appointment raviAppt = book(ravi, at(10, 0));
        Appointment anitaAppt = book(anita, at(10, 15));

        List<Long> order = queueService.queueFor(doctor.getId()).stream()
                .map(Appointment::getId).toList();

        assertEquals(List.of(raviAppt.getId(), anitaAppt.getId(), samAppt.getId()), order);
    }

    // ---------- Call next ----------

    @Test
    void callNextStartsFirstWaitingAndFinishesTheCurrentOne() {
        Appointment a = book(ravi, at(10, 0));
        Appointment b = book(anita, at(10, 15));
        appointmentService.markArrived(a.getId());
        appointmentService.markArrived(b.getId());

        Optional<Appointment> first = queueService.callNext(doctor.getId());
        assertTrue(first.isPresent());
        assertEquals(a.getId(), first.get().getId());
        assertEquals(AppointmentStatus.IN_PROGRESS, a.getStatus());
        assertNotNull(a.getStartedAt());

        Optional<Appointment> second = queueService.callNext(doctor.getId());
        assertEquals(b.getId(), second.get().getId());
        assertEquals(AppointmentStatus.COMPLETED, a.getStatus());
        assertEquals(a.getFinishedAt(), b.getStartedAt());   // finish of one = start of the next
    }

    @Test
    void callNextSkipsPatientWhoHasNotArrived() {
        Appointment atHome = book(ravi, at(10, 0));         // first in line, but still at home
        Appointment present = book(anita, at(10, 15));
        appointmentService.markArrived(present.getId());

        Optional<Appointment> called = queueService.callNext(doctor.getId());

        assertEquals(present.getId(), called.get().getId());
        assertEquals(AppointmentStatus.BOOKED, atHome.getStatus());   // keeps their place
    }

    @Test
    void callNextWithNobodyWaitingReturnsEmpty() {
        book(ravi, at(10, 0));   // booked but not arrived
        assertTrue(queueService.callNext(doctor.getId()).isEmpty());
    }

    // ---------- Wait estimate ----------

    @Test
    void estimateUsesDefaultWhenThereIsNoHistory() {
        book(ravi, at(10, 0));
        book(anita, at(10, 15));
        Appointment samAppt = book(sam, at(10, 30));

        QueueStatusResponse s = queueService.statusOf(samAppt.getId());

        assertEquals(2, s.patientsAhead());
        assertEquals(20L, s.estimatedWaitMinutes());   // 2 patients x default 10 minutes
    }

    @Test
    void averageIsLearnedFromRealConsultations() {
        LocalDateTime now = LocalDateTime.now();
        Appointment a = book(ravi, at(10, 0));
        a.setStatus(AppointmentStatus.COMPLETED);
        a.setStartedAt(now.minusMinutes(20));
        a.setFinishedAt(now.minusMinutes(16));   // 4 minutes
        Appointment b = book(anita, at(10, 15));
        b.setStatus(AppointmentStatus.COMPLETED);
        b.setStartedAt(now.minusMinutes(15));
        b.setFinishedAt(now.minusMinutes(9));    // 6 minutes

        assertEquals(5.0, queueService.averageConsultationMinutes(doctor.getId()), 0.001);
    }

    @Test
    void finishedPatientHasNoWaitNumbers() {
        Appointment a = book(ravi, at(10, 0));
        appointmentService.markArrived(a.getId());
        queueService.callNext(doctor.getId());   // Ravi starts
        queueService.callNext(doctor.getId());   // Ravi finishes, nobody else is waiting

        QueueStatusResponse s = queueService.statusOf(a.getId());

        assertEquals(AppointmentStatus.COMPLETED, s.status());
        assertNull(s.patientsAhead());
        assertNull(s.estimatedWaitMinutes());
    }
        // ---------- Cancel ----------

    @Test
    void cancelledSlotCanBeBookedAgain() {
        Appointment a = book(ravi, at(10, 0));
        appointmentService.cancel(a.getId());
        assertEquals(AppointmentStatus.CANCELLED, a.getStatus());

        Appointment b = book(anita, at(10, 0));   // same slot, now free
        assertEquals(AppointmentStatus.BOOKED, b.getStatus());
    }

    @Test
    void cancelledPatientIsNotInTheQueue() {
        Appointment a = book(ravi, at(10, 0));
        Appointment b = book(anita, at(10, 15));
        appointmentService.cancel(a.getId());

        List<Long> order = queueService.queueFor(doctor.getId()).stream()
                .map(Appointment::getId).toList();
        assertEquals(List.of(b.getId()), order);
    }

    @Test
    void cannotCancelTwiceOrAfterTheVisitStarted() {
        Appointment a = book(ravi, at(10, 0));
        appointmentService.cancel(a.getId());
        assertThrows(InvalidStateException.class, () -> appointmentService.cancel(a.getId()));

        Appointment b = book(anita, at(10, 15));
        appointmentService.markArrived(b.getId());
        queueService.callNext(doctor.getId());   // Anita is IN_PROGRESS now
        assertThrows(InvalidStateException.class, () -> appointmentService.cancel(b.getId()));
    }
}