package com.cognizant.hams.service;

import com.cognizant.hams.dto.request.DoctorAvailabilityDTO;
import com.cognizant.hams.dto.response.DoctorAndAvailabilityResponseDTO;
import com.cognizant.hams.dto.response.DoctorAvailabilityResponseDTO;
import jakarta.transaction.Transactional;

import java.util.List;

public interface DoctorAvailabilityService {

    //    // Availability Management
    // Add Availability
    @Transactional
    DoctorAvailabilityResponseDTO addAvailability(DoctorAvailabilityDTO slotDto);

    // Add Availability
    List<DoctorAvailabilityResponseDTO> getDoctorAvailability();

    @Transactional
    DoctorAvailabilityResponseDTO updateAvailability(Long availabilityId, DoctorAvailabilityDTO doctorAvailabilityDTO);

    @Transactional
    void deleteAvailability(Long availabilityId);

    DoctorAvailabilityResponseDTO updateAvailabilitySlot(Long doctorId, Long availabilityId, DoctorAvailabilityDTO doctorAvailabilityDTO);

    List<DoctorAndAvailabilityResponseDTO> getAvailableDoctor(String doctorName);

    List<DoctorAndAvailabilityResponseDTO> searchDoctorByName(String doctorName);

    List<DoctorAvailabilityResponseDTO> getDoctorAvailabilityByDate(Long doctorId, String date);

    List<DoctorAvailabilityResponseDTO> getDoctorAvailabilityByDate(String date);
}
