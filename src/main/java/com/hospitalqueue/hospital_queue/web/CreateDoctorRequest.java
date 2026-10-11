package com.hospitalqueue.hospital_queue.web;

import java.time.LocalTime;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateDoctorRequest(
        @NotBlank String name,
        @NotBlank String department,
        @NotNull LocalTime workStart,
        @NotNull LocalTime workEnd,
        @Min(5) int slotMinutes) {
}