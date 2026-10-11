package com.hospitalqueue.hospital_queue.web;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record BookingRequest(

        @NotNull Long doctorId,
        @NotNull Long patientId,
        @NotNull LocalDateTime slotTime)

{
}
