package com.cognizant.hams.controller;

import com.cognizant.hams.dto.request.MedicalRecordDTO;
import com.cognizant.hams.dto.response.MedicalRecordResponseDTO;
import com.cognizant.hams.service.MedicalRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;


@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MedicalRecordController {
    private final MedicalRecordService medicalRecordService;
    @PostMapping("/doctors/me/medical-records")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<MedicalRecordResponseDTO> createRecord(@Valid @RequestBody MedicalRecordDTO dto) {
        System.out.println("=== CREATE MEDICAL RECORD REQUEST ===");
        System.out.println("Appointment ID: " + dto.getAppointmentId());
        System.out.println("Patient ID: " + dto.getPatientId());
        System.out.println("Doctor ID: " + dto.getDoctorId());
        System.out.println("Diagnosis: " + dto.getDiagnosis());
        System.out.println("Prescriptions: " + (dto.getPrescriptions() != null ? dto.getPrescriptions().size() : 0));

        MedicalRecordResponseDTO saved = medicalRecordService.createRecord(dto);

        System.out.println("Medical record created successfully with ID: " + saved.getRecordId());
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }
    @GetMapping("/patients/me/medical-records")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<List<MedicalRecordResponseDTO>> getRecordsForPatient() {
        return ResponseEntity.ok(medicalRecordService.getRecordsForPatient());
    }

    @GetMapping("/doctors/me/medical-records") // ✨ Changed path to /doctors/me/...
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<List<MedicalRecordResponseDTO>> getRecordsForDoctor() {
        return ResponseEntity.ok(medicalRecordService.getRecordsForDoctor());
    }
}
