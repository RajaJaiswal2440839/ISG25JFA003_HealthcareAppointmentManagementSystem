package com.cognizant.hams.dto.request;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class RescheduleAppointmentRequestDTO { // New DTO name

    // Doctor ID is INTENTIONALLY OMITTED and derived from the security context

    @NotNull(message = "New appointment date is required")
    @FutureOrPresent(message = "Appointment date must be today or a future date")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate appointmentDate;

    @NotNull(message = "New start time is required")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
    private LocalTime startTime;

    @NotNull(message = "New end time is required")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
    private LocalTime endTime;
}