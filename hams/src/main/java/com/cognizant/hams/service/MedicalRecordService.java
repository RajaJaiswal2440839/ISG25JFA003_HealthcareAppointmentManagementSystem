package com.cognizant.hams.service;

import com.cognizant.hams.dto.request.MedicalRecordDTO;
import com.cognizant.hams.dto.response.MedicalRecordResponseDTO;

import java.util.List;
public interface MedicalRecordService {
    MedicalRecordResponseDTO createRecord(MedicalRecordDTO dto);
    List<MedicalRecordResponseDTO> getRecordsForPatient();
    List<MedicalRecordResponseDTO> getRecordsForDoctor();
}