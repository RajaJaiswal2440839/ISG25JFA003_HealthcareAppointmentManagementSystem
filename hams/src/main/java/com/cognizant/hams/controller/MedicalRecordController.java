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
        MedicalRecordResponseDTO saved = medicalRecordService.createRecord(dto);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }
    @GetMapping("/patients/me/medical-records")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<List<MedicalRecordResponseDTO>> getRecordsForPatient() {
        return ResponseEntity.ok(medicalRecordService.getRecordsForPatient());
    }

    @GetMapping("/doctors/{doctorId}/medical-records")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<List<MedicalRecordResponseDTO>> getRecordsForDoctor(@PathVariable Long doctorId) {
        return ResponseEntity.ok(medicalRecordService.getRecordsForDoctor(doctorId));
    }
}
