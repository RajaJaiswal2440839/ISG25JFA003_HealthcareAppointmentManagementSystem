package com.cognizant.hams.service.impl;

import com.cognizant.hams.dto.request.MedicalRecordDTO;
import com.cognizant.hams.dto.response.MedicalRecordResponseDTO;
import com.cognizant.hams.dto.response.PrescriptionResponseDTO;
import com.cognizant.hams.service.MedicalRecordService;
import com.cognizant.hams.entity.*;
import com.cognizant.hams.exception.APIException;
import com.cognizant.hams.exception.ResourceNotFoundException;
import com.cognizant.hams.repository.*;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MedicalRecordServiceImpl implements MedicalRecordService {
    private final MedicalRecordRepository medicalRecordRepository;
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public MedicalRecordResponseDTO createRecord(MedicalRecordDTO dto) {
        // Get logged-in doctor
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        Doctor loggedInDoctor = (Doctor) doctorRepository.findByUser_Username(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "username", currentUsername));

        // Validate Appointment
        Appointment appointment = appointmentRepository.findById(dto.getAppointmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "Id", dto.getAppointmentId()));

        Patient patient = patientRepository.findById(dto.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "Id", dto.getPatientId()));

        // Verify the appointment belongs to this doctor
        if (!appointment.getDoctor().getDoctorId().equals(loggedInDoctor.getDoctorId())) {
            throw new AccessDeniedException("You can only create records for your own appointments");
        }

        // Verify appointment matches patient and doctor
        if (!appointment.getPatient().getPatientId().equals(patient.getPatientId())) {
            throw new APIException("Appointment does not belong to the specified patient");
        }

        // Check if appointment is confirmed (can only complete confirmed appointments)
        if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new APIException("Only CONFIRMED appointments can be completed. Current status: " + appointment.getStatus());
        }

        // Create Medical Record
        MedicalRecord record = new MedicalRecord();
        record.setPatient(patient);
        record.setDoctor(loggedInDoctor);
        record.setReason(dto.getReason());
        record.setDiagnosis(dto.getDiagnosis());
        record.setNotes(dto.getNotes());

        // Add prescriptions if provided
        if (dto.getPrescriptions() != null && !dto.getPrescriptions().isEmpty()) {
            List<Prescription> prescriptions = dto.getPrescriptions().stream()
                    .map(pDto -> {
                        Prescription p = modelMapper.map(pDto, Prescription.class);
                        p.setMedicalRecord(record);
                        return p;
                    }).collect(Collectors.toList());
            record.setPrescriptions(prescriptions);
        }

        // Save medical record
        MedicalRecord saved = medicalRecordRepository.save(record);

        // Mark appointment as COMPLETED
        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointmentRepository.save(appointment);

        System.out.println("Medical record created with ID: " + saved.getRecordId());
        System.out.println("Appointment " + appointment.getAppointmentId() + " marked as COMPLETED");

        return toDto(saved);
    }

    @Override
    public List<MedicalRecordResponseDTO> getRecordsForPatient() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        Patient patient = (Patient) patientRepository.findByUser_Username(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "username", currentUsername));

        List<MedicalRecord> medicalRecords = medicalRecordRepository
                .findByPatient_PatientIdOrderByCreatedAtDesc(patient.getPatientId());

        return medicalRecords.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<MedicalRecordResponseDTO> getRecordsForDoctor() {

        // Get logged-in doctor
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        Doctor loggedInDoctor = (Doctor) doctorRepository.findByUser_Username(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "username", currentUsername));

        Long secureDoctorId = loggedInDoctor.getDoctorId();

        List<MedicalRecord> records = medicalRecordRepository
                .findByDoctor_DoctorIdOrderByCreatedAtDesc(secureDoctorId); // Use secureDoctorId

        return records.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private MedicalRecordResponseDTO toDto(MedicalRecord record) {
        MedicalRecordResponseDTO resp = new MedicalRecordResponseDTO();
        resp.setRecordId(record.getRecordId());
        resp.setPatientId(record.getPatient().getPatientId());
        resp.setDoctorId(record.getDoctor().getDoctorId());
        resp.setPatientName(record.getPatient().getName());
        resp.setDoctorName(record.getDoctor().getDoctorName());
        resp.setReason(record.getReason());
        resp.setDiagnosis(record.getDiagnosis());
        resp.setNotes(record.getNotes());
        resp.setCreatedAt(record.getCreatedAt());

        if (record.getPrescriptions() != null) {
            List<PrescriptionResponseDTO> prescriptionDTOs = record.getPrescriptions().stream()
                    .map(p -> modelMapper.map(p, PrescriptionResponseDTO.class))
                    .collect(Collectors.toList());
            resp.setPrescriptions(prescriptionDTOs);
        }
        return resp;
    }
}