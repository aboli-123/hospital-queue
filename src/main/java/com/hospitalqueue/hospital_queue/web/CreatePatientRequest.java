package com.hospitalqueue.hospital_queue.web;

import jakarta.validation.constraints.NotBlank;


public record CreatePatientRequest(
      @NotBlank String name,
      @NotBlank String phone) {
} 

