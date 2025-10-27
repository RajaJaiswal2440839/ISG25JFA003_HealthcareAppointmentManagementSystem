
package com.cognizant.hams.service;

import com.cognizant.hams.dto.request.AppointmentDTO;
import com.cognizant.hams.dto.request.RescheduleAppointmentRequestDTO;
import com.cognizant.hams.dto.response.AppointmentResponseDTO;
import jakarta.transaction.Transactional;

import java.util.List;

public interface AppointmentService {
    AppointmentResponseDTO confirmAppointment(Long appointmentId);
    AppointmentResponseDTO rejectAppointment(Long appointmentId, String reason);

    List<AppointmentResponseDTO> getAppointmentsForDoctor();

    AppointmentResponseDTO bookAppointment(AppointmentDTO appointmentDTO);

    @Transactional
    AppointmentResponseDTO rescheduleAppointment(Long appointmentId, RescheduleAppointmentRequestDTO appointmentDTO);

    AppointmentResponseDTO cancelAppointment(Long appointmentId);
    AppointmentResponseDTO getAppointmentById(Long appointmentId);
    List<AppointmentResponseDTO> getAppointmentsForPatient();

    long getTodayAppointmentCount();

    long getPendingReviewsCountForDoctor(String username);

    List<AppointmentResponseDTO> getTodayAppointmentsForDoctor();

    Long getTotalPatientCountForCurrentDoctor();
}
