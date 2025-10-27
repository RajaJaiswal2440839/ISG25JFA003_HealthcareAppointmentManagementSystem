
package com.cognizant.hams.service.impl;

import com.cognizant.hams.dto.request.AppointmentDTO;
import com.cognizant.hams.dto.request.RescheduleAppointmentRequestDTO;
import com.cognizant.hams.dto.response.AppointmentResponseDTO;
import com.cognizant.hams.entity.Appointment;
import com.cognizant.hams.entity.AppointmentStatus;
import com.cognizant.hams.entity.Doctor;
import com.cognizant.hams.entity.Patient;
import com.cognizant.hams.exception.APIException;
import com.cognizant.hams.exception.ResourceNotFoundException;
import com.cognizant.hams.repository.AppointmentRepository;
import com.cognizant.hams.repository.DoctorAvailabilityRepository;
import com.cognizant.hams.repository.DoctorRepository;
import com.cognizant.hams.repository.PatientRepository;
import com.cognizant.hams.service.AppointmentService;
import com.cognizant.hams.service.NotificationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final NotificationService notificationService;
    private final ModelMapper modelMapper;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final DoctorAvailabilityRepository doctorAvailabilityRepository;

    @Override
    @Transactional
    public AppointmentResponseDTO rejectAppointment(Long appointmentId, String reason) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        Doctor loggedInDoctor = (Doctor) doctorRepository.findByUser_Username(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "username", currentUsername));

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", appointmentId));

        if (!appointment.getDoctor().getDoctorId().equals(loggedInDoctor.getDoctorId())) {
            throw new AccessDeniedException("Doctor is not authorized to update this appointment.");
        }
        appointment.setStatus(AppointmentStatus.REJECTED);
        Appointment saved = appointmentRepository.save(appointment);
        notificationService.notifyPatientOnAppointmentDecision(saved, false, reason);
        return modelMapper.map(saved, AppointmentResponseDTO.class);
    }

    @Override
    @Transactional
    public AppointmentResponseDTO confirmAppointment(Long appointmentId) {
        // 1. Get the authenticated doctor's username from the security context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        Doctor loggedInDoctor = (Doctor) doctorRepository.findByUser_Username(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "username", currentUsername));

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", appointmentId));

        if (!appointment.getDoctor().getDoctorId().equals(loggedInDoctor.getDoctorId())) {
            throw new AccessDeniedException("Doctor is not authorized to update this appointment.");
        }

        // 5. Update appointment status and save
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        Appointment saved = appointmentRepository.save(appointment);

        // 6. Notify the patient
        notificationService.notifyPatientOnAppointmentDecision(saved, true, null);

        return modelMapper.map(saved, AppointmentResponseDTO.class);
    }
    @Override
    public List<AppointmentResponseDTO> getAppointmentsForPatient() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        Patient patient = (Patient) patientRepository.findByUser_Username(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "username", currentUsername));

        List<Appointment> appointments = appointmentRepository.findByPatient_PatientId(patient.getPatientId());

        return appointments.stream()
                .map(appointment -> modelMapper.map(appointment, AppointmentResponseDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<AppointmentResponseDTO> getAppointmentsForDoctor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        Doctor doctor = doctorRepository.findByUser_Username(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "username", currentUsername));

        List<Appointment> appointments = appointmentRepository.findByDoctor_DoctorId(doctor.getDoctorId());

        return appointments.stream()
                .map(appointment -> modelMapper.map(appointment, AppointmentResponseDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public AppointmentResponseDTO bookAppointment(AppointmentDTO appointmentDTO) {
        // 1. Get current authenticated patient
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        Patient patient = (Patient) patientRepository.findByUser_Username(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "username", currentUsername));

        // 2. Get Doctor
        Doctor doctor = doctorRepository.findById(appointmentDTO.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "Id", appointmentDTO.getDoctorId()));

        // VALIDATION 1: Check if doctor has availability for this slot
        // FIX: Pass LocalDate object directly from DTO (assuming DTO handles it)
        boolean hasAvailability = doctorAvailabilityRepository.existsByDoctorDoctorIdAndAvailableDateAndStartTimeAndEndTime(
                appointmentDTO.getDoctorId(),
                appointmentDTO.getAppointmentDate(), // Corrected: Pass LocalDate directly
                appointmentDTO.getStartTime(),
                appointmentDTO.getEndTime()
        );

        if (!hasAvailability) {
            throw new APIException("This time slot is not available for the selected doctor.");
        }

        // VALIDATION 2: Check if slot is already booked (Confirmed or Pending)
        boolean isSlotBooked = appointmentRepository.existsByDoctorDoctorIdAndAppointmentDateAndStartTimeAndStatus(
                appointmentDTO.getDoctorId(),
                appointmentDTO.getAppointmentDate(),
                appointmentDTO.getStartTime(),
                AppointmentStatus.CONFIRMED
        );

        if (isSlotBooked) {
            throw new APIException("This time slot has already been booked (Confirmed).");
        }

        // 3. Create and Save Appointment
        Appointment appointment = modelMapper.map(appointmentDTO, Appointment.class);
        appointment.setAppointmentId(null);
        appointment.setVersion(null);
        appointment.setStatus(AppointmentStatus.PENDING); // Set initial status
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);

        Appointment savedAppointment = appointmentRepository.save(appointment);

        // 4. Notify Doctor
        notificationService.notifyDoctorOnAppointmentRequest(savedAppointment);

        // 5. Return Response
        return modelMapper.map(savedAppointment, AppointmentResponseDTO.class);
    }

    @Override
    @Transactional
    public AppointmentResponseDTO rescheduleAppointment(Long appointmentId, RescheduleAppointmentRequestDTO rescheduleDTO) {
        // Get the logged-in doctor
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        Doctor loggedInDoctor = (Doctor) doctorRepository.findByUser_Username(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "username", currentUsername));

        // Get the appointment
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "appointmentId", appointmentId));

        // Verify this appointment belongs to the logged-in doctor
        if (!appointment.getDoctor().getDoctorId().equals(loggedInDoctor.getDoctorId())) {
            throw new AccessDeniedException("You can only reschedule your own appointments");
        }

        // Verify appointment is in CONFIRMED or PENDING status (can't reschedule completed/cancelled)
        if (appointment.getStatus() != AppointmentStatus.CONFIRMED &&
                appointment.getStatus() != AppointmentStatus.PENDING) {
            throw new APIException("Only CONFIRMED or PENDING appointments can be rescheduled");
        }

        // Check if the new time slot is available for this doctor
        boolean hasAvailability = doctorAvailabilityRepository.existsByDoctorDoctorIdAndAvailableDateAndStartTimeAndEndTime(
                loggedInDoctor.getDoctorId(),
                rescheduleDTO.getAppointmentDate(),
                rescheduleDTO.getStartTime(),
                rescheduleDTO.getEndTime()
        );

        if (!hasAvailability) {
            throw new APIException("The selected time slot is not available");
        }

        // Check if the new slot is already booked
        boolean isSlotBooked = appointmentRepository.existsByDoctorIdAndDateTimeAndStatusNotExcludingId(
                loggedInDoctor.getDoctorId(),
                rescheduleDTO.getAppointmentDate(),
                rescheduleDTO.getStartTime(),
                AppointmentStatus.REJECTED,
                appointmentId // Exclude current appointment
        );

        if (isSlotBooked) {
            throw new APIException("This time slot has already been booked");
        }

        // Update the appointment
        appointment.setAppointmentDate(rescheduleDTO.getAppointmentDate());
        appointment.setStartTime(rescheduleDTO.getStartTime());
        appointment.setEndTime(rescheduleDTO.getEndTime());

        Appointment updatedAppointment = appointmentRepository.save(appointment);

        // Optional: Send notification to patient about reschedule
        // notificationService.notifyPatientOnReschedule(updatedAppointment);

        return modelMapper.map(updatedAppointment, AppointmentResponseDTO.class);
    }

    @Override
    public AppointmentResponseDTO cancelAppointment(Long appointmentId) {
        Appointment appointmentToCancel = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "Id", appointmentId));

        if (appointmentToCancel.getStatus() == AppointmentStatus.COMPLETED || appointmentToCancel.getStatus() == AppointmentStatus.CANCELED) {
            throw new APIException("Appointment cannot be canceled as it is already " + appointmentToCancel.getStatus());
        }

        appointmentToCancel.setStatus(AppointmentStatus.CANCELED);
        Appointment canceledAppointment = appointmentRepository.save(appointmentToCancel);

        return modelMapper.map(canceledAppointment, AppointmentResponseDTO.class);
    }

    @Override
    public AppointmentResponseDTO getAppointmentById(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "Id", appointmentId));
        return modelMapper.map(appointment, AppointmentResponseDTO.class);
    }

    @Override
    public long getTodayAppointmentCount() {
        // Get today's date
        LocalDate today = LocalDate.now();

        // Use the new repository method to count appointments for today
        return appointmentRepository.countByAppointmentDate(today);
    }

    @Override
    public long getPendingReviewsCountForDoctor(String username) {
        // 1. Find the logged-in doctor to get their ID
        Doctor loggedInDoctor = (Doctor) doctorRepository.findByUser_Username(username)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "username", username));

        // 2. Define criteria
        LocalDate today = LocalDate.now();
        AppointmentStatus pending = AppointmentStatus.PENDING;

        // 3. Use the new repository method
        return appointmentRepository.countByDoctor_DoctorIdAndStatusAndAppointmentDateGreaterThanEqual(
                loggedInDoctor.getDoctorId(),
                pending,
                today // Filters for today or any future date
        );
    }


    @Override
    public List<AppointmentResponseDTO> getTodayAppointmentsForDoctor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        Doctor doctor = (Doctor) doctorRepository.findByUser_Username(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "username", currentUsername));

        // 🔑 Use the new repository method
        List<Appointment> appointments = appointmentRepository.findByDoctor_DoctorIdAndAppointmentDate(
                doctor.getDoctorId(),
                LocalDate.now()
        );

        return appointments.stream()
                .map(appointment -> modelMapper.map(appointment, AppointmentResponseDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public Long getTotalPatientCountForCurrentDoctor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        Doctor loggedInDoctor = doctorRepository.findByUser_Username(currentUsername)
                .orElseThrow(()-> new ResourceNotFoundException("Doctor", "username", currentUsername));

        return appointmentRepository.countDistinctPatientByDoctorId(loggedInDoctor.getDoctorId());

    }
}