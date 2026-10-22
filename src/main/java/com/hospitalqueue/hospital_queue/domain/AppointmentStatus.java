package com.hospitalqueue.hospital_queue.domain;



public enum AppointmentStatus {
    BOOKED,       // slot reserved, patient not at the hospital yet
    WAITING,      // patient arrived, in the queue
    IN_PROGRESS,  // doctor is seeing the patient
    COMPLETED,
    NO_SHOW,
    CANCELLED

 
}