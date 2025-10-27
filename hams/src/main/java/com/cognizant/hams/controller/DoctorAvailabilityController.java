package com.cognizant.hams.controller;

import com.cognizant.hams.dto.request.DoctorAvailabilityDTO;
import com.cognizant.hams.dto.response.DoctorAndAvailabilityResponseDTO;
import com.cognizant.hams.dto.response.DoctorAvailabilityResponseDTO;
import com.cognizant.hams.service.DoctorAvailabilityService;
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
public class DoctorAvailabilityController {

    private final DoctorAvailabilityService doctorAvailabilityService;

    // POST: Create new slot (Used by Angular saveNewSlot)
    @PostMapping("/doctors/availability")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<DoctorAvailabilityResponseDTO> addAvailability(@Valid @RequestBody DoctorAvailabilityDTO slotDto) {
        DoctorAvailabilityResponseDTO savedSlot = doctorAvailabilityService.addAvailability(slotDto);
        return new ResponseEntity<>(savedSlot, HttpStatus.CREATED);
    }

    // GET: Get all slots for current doctor (Used by Angular fetchScheduledSlots)
    @GetMapping("/doctors/availability")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<List<DoctorAvailabilityResponseDTO>> getDoctorAvailability() {
        List<DoctorAvailabilityResponseDTO> availability = doctorAvailabilityService.getDoctorAvailability();
        return new ResponseEntity<>(availability, HttpStatus.OK);
    }

    // PUT: Update slot by availabilityId (Recommended for DOCTOR role, uses internal doctor ID)
    @PutMapping("/doctors/availability/{availabilityId}")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<DoctorAvailabilityResponseDTO> updateAvailabilitySlotDoctor(
            @PathVariable("availabilityId") Long availabilityId,
            @Valid @RequestBody DoctorAvailabilityDTO doctorAvailabilityDTO) {
        // Calls the service method that derives the doctorId from security context
        DoctorAvailabilityResponseDTO doctorResponseDTO = doctorAvailabilityService.updateAvailability(availabilityId, doctorAvailabilityDTO);
        return new ResponseEntity<>(doctorResponseDTO, HttpStatus.OK);
    }

    // DELETE: Delete slot by availabilityId (Fixes the Angular delete error)
    @DeleteMapping("/doctors/availability/{availabilityId}")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<Void> deleteAvailability(@PathVariable("availabilityId") Long availabilityId) {
        doctorAvailabilityService.deleteAvailability(availabilityId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT); // 204 No Content
    }

    // Existing ADMIN Update endpoint (Keeping for compatibility, though not used by your frontend code)
    @PutMapping("/admin/{doctorId}/availability/{availabilityId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DoctorAvailabilityResponseDTO> updateAvailabilitySlotAdmin(
            @PathVariable("doctorId") Long doctorId,
            @PathVariable("availabilityId") Long availabilityId,
            @RequestBody DoctorAvailabilityDTO doctorAvailabilityDTO) {
        DoctorAvailabilityResponseDTO doctorResponseDTO = doctorAvailabilityService.updateAvailabilitySlot(doctorId, availabilityId, doctorAvailabilityDTO);
        return new ResponseEntity<>(doctorResponseDTO, HttpStatus.OK);
    }

    // Existing Patient Endpoints
    @GetMapping("/patients/doctor-availability")
    public ResponseEntity<List<DoctorAndAvailabilityResponseDTO>> getAvailableDoctor(@RequestParam("name") String doctorName){
        List<DoctorAndAvailabilityResponseDTO> doctorAndAvailabilityResponseDTOList = doctorAvailabilityService.getAvailableDoctor(doctorName);
        return new ResponseEntity<>(doctorAndAvailabilityResponseDTOList, HttpStatus.OK);
    }

    @GetMapping("/patients/searchDoctor")
    public ResponseEntity<List<DoctorAndAvailabilityResponseDTO>> searchDoctorByName(@RequestParam("name") String doctorName){
        List<DoctorAndAvailabilityResponseDTO> doctorAndAvailabilityResponseDTOList = doctorAvailabilityService.searchDoctorByName(doctorName);
        return new ResponseEntity<>(doctorAndAvailabilityResponseDTOList, HttpStatus.OK);
    }

    @GetMapping("/patients/doctor-availability/{doctorId}")
    public ResponseEntity<List<DoctorAvailabilityResponseDTO>> getDoctorAvailabilityByDate(
            @PathVariable("doctorId") Long doctorId,
            @RequestParam("date") String date) {
        List<DoctorAvailabilityResponseDTO> availability =
                doctorAvailabilityService.getDoctorAvailabilityByDate(doctorId, date);
        return new ResponseEntity<>(availability, HttpStatus.OK);
    }

    @GetMapping("/doctor/availability")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<List<DoctorAvailabilityResponseDTO>> getDoctorAvailability(
            @RequestParam(required = false) String date) {

        List<DoctorAvailabilityResponseDTO> availability;
        if (date != null && !date.isEmpty()) {
            // Filter by date
            availability = doctorAvailabilityService.getDoctorAvailabilityByDate(date);
        } else {
            // Get all availability
            availability = doctorAvailabilityService.getDoctorAvailability();
        }
        return new ResponseEntity<>(availability, HttpStatus.OK);
    }
}